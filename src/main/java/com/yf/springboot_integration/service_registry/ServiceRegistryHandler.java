package com.yf.springboot_integration.service_registry;

import com.yf.pool.threadpool.ThreadPool;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yyf
 * @date 2025/8/20 0:20
 * @description
 */
@Component
@ConditionalOnProperty(prefix = "yf.service-registry",name = "enabled",havingValue = "true")
public class ServiceRegistryHandler {

    //    每个节点信息包含字段：ip、port、cpuUsage(cpu使用率)、memoryUsage（内存使用率）、taskNums（任务数量）、queueCapacity(队列大小)
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ThreadPool threadPool;
    @Autowired
    private ServerProperties serverProperties;

    private static String IP;
    private static String PORT;
    private static String KEY;
    //redis中的key
    private final static String REGISTRY_KEY_PREFIX = "task_flow:registry";//基础信息注册  hash 真正的key是前缀加节点的ip和进程号
    private final static String SORT_BY_CPU = "task_flow:sort:cpuUsage";//节点按cpu使用率排序
    private final static String SORT_BY_MEMORY = "task_flow:sort:memoryUsage";//节点按内存使用率排序
    private final static String SORT_BY_Queue = "task_flow:sort:queueUsage";//节点按队列使用率排序
    private final static List<String> zsetKeys = Arrays.asList(SORT_BY_CPU, SORT_BY_MEMORY, SORT_BY_Queue);
    // Lua脚本
    DefaultRedisScript<Long> redisScript;

    private final static  int cpuCores = Runtime.getRuntime().availableProcessors();
    private final static int HEART_BEAT = 10;
    private final static int EXPIRE = 12;

    @PostConstruct
    public void firstRegister(){//启动服务，向redis进行注册
        InetAddress localHost;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        IP = localHost.getHostAddress();
        PORT = serverProperties.getPort().toString();
        KEY = REGISTRY_KEY_PREFIX+":"+IP+ ":"+ PORT;
        register();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {//进程退出时，删除redis中hash的key 以及zset的元素
            stringRedisTemplate.opsForHash().delete(KEY,"ip", "port", "cpuUsage", "memoryUsage", "taskNums", "queueCapacity");
            stringRedisTemplate.opsForZSet().remove(SORT_BY_CPU,KEY);
            stringRedisTemplate.opsForZSet().remove(SORT_BY_MEMORY,KEY);
            stringRedisTemplate.opsForZSet().remove(SORT_BY_Queue,KEY);
        }));
        // 加载并预编译Lua脚本
        loadAndPrecompileLuaScript();
    }

    @Scheduled(fixedDelay = HEART_BEAT*1000)
    public void heartBeating(){//心跳机制
        register();
        //清理ZSet中无效元素
        stringRedisTemplate.execute(
                redisScript,
                zsetKeys
        );
    }

    public void register(){//注册服务
        double cpuUsage = getCpuUsage();
        Map<String,String> map = new HashMap<>();
        map.put("ip", IP);
        map.put("port", PORT);
        double memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100.0 / Runtime.getRuntime().maxMemory();
        map.put("cpuUsage", String.valueOf(cpuUsage));
        map.put("memoryUsage", String.valueOf(memoryUsage));
        int taskNums = threadPool.getPartition().getEleNums();
        map.put("taskNums",String.valueOf(taskNums));
        Integer capacity = threadPool.getPartition().getCapacity();
        map.put("queueCapacity", String.valueOf(capacity));
        stringRedisTemplate.opsForHash().putAll(KEY, map);
        stringRedisTemplate.expire(KEY, EXPIRE, TimeUnit.SECONDS);
        stringRedisTemplate.opsForZSet().add(SORT_BY_CPU, KEY, cpuUsage);
        stringRedisTemplate.opsForZSet().add(SORT_BY_MEMORY, KEY, memoryUsage);
        stringRedisTemplate.opsForZSet().add(SORT_BY_Queue, KEY, capacity==null?0: (double) taskNums /capacity);
        stringRedisTemplate.expire(SORT_BY_CPU, EXPIRE, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SORT_BY_MEMORY, EXPIRE, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SORT_BY_Queue, EXPIRE, TimeUnit.SECONDS);
    }

    //加载并预编译Lua脚本
    private void loadAndPrecompileLuaScript() {
        try {
            // 加载resources目录下的Lua脚本文件
            Resource resource = resourceLoader.getResource("classpath:lua/cleanup_zset.lua");
            byte[] scriptBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            redisScript = new DefaultRedisScript<>(new String(scriptBytes, StandardCharsets.UTF_8), Long.class);
        } catch (IOException e) {
            throw new RuntimeException("lua脚本加载失败", e);
        }
    }

    public double getCpuUsage(){//获取cpu使用率
        long startTime = System.currentTimeMillis();
        long cpuStartTime = ProcessHandle.current().info().totalCpuDuration().get().toMillis();
        try {
            Thread.sleep(1000);//休眠1秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long endTime = System.currentTimeMillis();
        long cpuEndTime = ProcessHandle.current().info().totalCpuDuration().get().toMillis();
        return (cpuEndTime - cpuStartTime) * 100.0 / (endTime - startTime)/cpuCores;
    }
}
