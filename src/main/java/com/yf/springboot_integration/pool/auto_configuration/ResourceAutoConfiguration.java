package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.common.constant.Logo;
import com.yf.core.resource_container.scanned_annotation.ResourceScan;
import com.yf.core.resource_container.ResourceScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author yyf
 * @description 资源扫描自动配置
 *
 * 当Spring Boot应用启动时，自动扫描并注册所有标注了@ResourceScan的类所在包及子包下的资源
 *
 * 使用方式：
 * <pre>
 * // 在SpringBootApplication入口类上添加注解即可自动扫描
 * &#64;ResourceScan
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Slf4j
@AutoConfiguration
public class ResourceAutoConfiguration {

    /**
     * 资源扫描初始化器
     * 在Spring容器初始化完成后，自动扫描所有被@ResourceScan注解的类
     */
    @Bean
    public ResourceScannerInitializer resourceScannerInitializer(ApplicationContext context) {
        return new ResourceScannerInitializer(context);
    }

    /**
     * 资源扫描初始化器类
     */
    public static class ResourceScannerInitializer {

        public ResourceScannerInitializer(ApplicationContext context) {
            // 获取Spring容器中所有bean
            String[] beanNames = context.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                try {
                    Class<?> beanClass = context.getType(beanName);
                    if (beanClass != null && beanClass.isAnnotationPresent(ResourceScan.class)) {
                        log.info(Logo.LOG_LOGO + "检测到@ResourceScan注解，开始扫描资源: {}", beanClass.getName());
                        ResourceScanner.scan(beanClass);
                        log.info(Logo.LOG_LOGO + "资源扫描完成");
                        break; // 找到一个就足够了
                    }
                } catch (Exception e) {
                    // 忽略无法获取类型的bean
                }
            }
        }
    }
}
