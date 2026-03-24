package com.yf.core.resource_container;

import com.yf.common.constant.Constant;
import com.yf.common.constant.Logo;
import com.yf.common.task.GCTask;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.SchedulePolicy;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.resource_container.resource_manager.GCTaskManager;
import com.yf.core.resource_container.resource_manager.PartiResourceManager;
import com.yf.core.resource_container.resource_manager.RSResourceManager;
import com.yf.core.resource_container.resource_manager.SPResourceManager;
import com.yf.core.resource_container.scanned_annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yyf
 * @description 资源扫描器
 *
 * 扫描@ResourceScan注解所在包及子包下的所有类，
 * 将被@PartiResource、@SPResource、@RSResource、@GCTResource注解的类注册到对应管理器
 *
 * 使用方式（只需添加@ResourceScan注解，自动扫描）：
 * <pre>
 * &#64;ResourceScan
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         // 自动扫描包下的所有资源
 *         ThreadPool tp = new ThreadPool(...);
 *     }
 * }
 * </pre>
 */
@Slf4j
public class ResourceScanner {

    /**
     * 是否已扫描标志
     */
    private static volatile boolean scanned = false;

    /**
     * 扫描锁
     */
    private static final Object SCAN_LOCK = new Object();

    /**
     * 确保已执行扫描（在任何DynaPart组件首次使用前自动调用）
     */
    public static void ensureScanned() {
        if (!scanned) {
            synchronized (SCAN_LOCK) {
                if (!scanned) {
                    // 遍历类路径，查找所有包含@ResourceScan标注类的目录
                    String classPath = System.getProperty("java.class.path");
                    String[] paths = classPath.split(File.pathSeparator);

                    for (String path : paths) {
                        try {
                            File file = new File(path);
                            if (file.isDirectory()) {
                                scanDirectoryForResourceScan(file, "");
                            }
                        } catch (Exception e) {
                            // 忽略扫描错误
                        }
                    }

                    scanned = true;
                }
            }
        }
    }

    /**
     * 从目录中查找@ResourceScan标注的类并扫描
     */
    private static void scanDirectoryForResourceScan(File dir, String packageName) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectoryForResourceScan(file, newPackage);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName.isEmpty()
                    ? file.getName().replace(".class", "")
                    : packageName + "." + file.getName().replace(".class", "");

                try {
                    Class<?> clazz = Class.forName(className, false,
                            Thread.currentThread().getContextClassLoader());

                    if (clazz.isAnnotationPresent(ResourceScan.class)) {
                        // 找到@ResourceScan标注的类，扫描其所在包
                        log.info(Logo.LOG_LOGO + "检测到@ResourceScan注解: {}", clazz.getName());
                        scan(clazz);
                        return; // 找到一个就退出
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // 忽略
                }
            }
        }
    }

    /**
     * 扫描并注册资源
     * @param scanClass 标注了@ResourceScan的类
     */
    public static void scan(Class<?> scanClass) {
        if (scanClass == null) {
            return;
        }

        Package scanPackage = scanClass.getPackage();
        if (scanPackage == null) {
            log.warn("无法获取{}所在包", scanClass.getName());
            return;
        }

        String packageName = scanPackage.getName();
        String packagePath = packageName.replace('.', '/');

        try {
            ClassLoader classLoader = scanClass.getClassLoader();
            URL packageURL = classLoader.getResource(packagePath);
            if (packageURL == null) {
                log.warn("无法找到包: {}", packageName);
                return;
            }

            List<Class<?>> annotatedClasses = new ArrayList<>();
            List<Class<?>> gcTaskClasses = new ArrayList<>();

            // 只扫描文件目录
            scanFilePackage(packageURL.getFile(), packageName, annotatedClasses, gcTaskClasses);

            // 注册非GCTask的资源
            for (Class<?> clazz : annotatedClasses) {
                registerResource(clazz);
            }

            // 注册GCTask资源（需要先确保绑定资源已注册）
            for (Class<?> clazz : gcTaskClasses) {
                registerGCTask(clazz);
            }

        } catch (Exception e) {
            log.error("扫描包{}时出错", packageName, e);
        }
    }

    private static void scanFilePackage(String basePath, String packageName,
                                         List<Class<?>> annotatedClasses, List<Class<?>> gcTaskClasses) {
        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return;
        }
        scanDirectory(baseDir, packageName, annotatedClasses, gcTaskClasses);
    }

    private static void scanDirectory(File dir, String packageName,
                                      List<Class<?>> annotatedClasses, List<Class<?>> gcTaskClasses) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(),
                        annotatedClasses, gcTaskClasses);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                loadAndClassify(className, annotatedClasses, gcTaskClasses);
            }
        }
    }

    private static void loadAndClassify(String className,
                                         List<Class<?>> annotatedClasses, List<Class<?>> gcTaskClasses) {
        try {
            Class<?> clazz = Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());

            if (clazz.isAnnotationPresent(GCTResource.class)) {
                gcTaskClasses.add(clazz);
            } else if (clazz.isAnnotationPresent(PartiResource.class) ||
                    clazz.isAnnotationPresent(SPResource.class) ||
                    clazz.isAnnotationPresent(RSResource.class)) {
                annotatedClasses.add(clazz);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // 忽略无法加载的类
        }
    }

    private static void registerResource(Class<?> clazz) {
        PartiResource partiResource = clazz.getAnnotation(PartiResource.class);
        RSResource rsResource = clazz.getAnnotation(RSResource.class);
        SPResource spResource = clazz.getAnnotation(SPResource.class);

        if (partiResource != null) {
            PartiResourceManager.register(partiResource.value(),
                    clazz.asSubclass(Partition.class));
            log.info(Logo.LOG_LOGO + "自定义队列注册: name={}, class={}",
                    partiResource.value(), clazz.getName());
        } else if (rsResource != null) {
            RSResourceManager.register(rsResource.value(),
                    clazz.asSubclass(RejectStrategy.class));
            log.info(Logo.LOG_LOGO + "自定义拒绝策略注册: name={}, class={}",
                    rsResource.value(), clazz.getName());
        } else if (spResource != null) {
            SPResourceManager.register(spResource.value(), clazz);
            log.info(Logo.LOG_LOGO + "自定义调度策略注册: name={}, class={}",
                    spResource.value(), clazz.getName());
        }
    }

    private static void registerGCTask(Class<?> clazz) {
        GCTResource gctResource = clazz.getAnnotation(GCTResource.class);

        String partiName = gctResource.bindingPartiResource();
        String spName = gctResource.bindingSPResource();
        String spType = gctResource.spType();

        Class<? extends Partition> parti = PartiResourceManager.getResource(partiName);

        Class<? extends SchedulePolicy> spResource = null;
        if (Constant.POLL.equals(spType)) {
            spResource = SPResourceManager.getPollResource(spName);
        } else if (Constant.OFFER.equals(spType)) {
            spResource = SPResourceManager.getOfferResource(spName);
        } else if (Constant.REMOVE.equals(spType)) {
            spResource = SPResourceManager.getRemoveResource(spName);
        }

        if (parti != null) {
            GCTaskManager.register(parti, clazz.asSubclass(GCTask.class));
            log.info(Logo.LOG_LOGO + "GCTask注册: bindingParti={}", partiName);
        }
        if (spResource != null) {
            GCTaskManager.register(spResource, clazz.asSubclass(GCTask.class));
            log.info(Logo.LOG_LOGO + "GCTask注册: bindingSP={}, type={}", spName, spType);
        }
    }
}
