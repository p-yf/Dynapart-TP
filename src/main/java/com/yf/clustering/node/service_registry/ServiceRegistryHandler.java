package com.yf.clustering.node.service_registry;

import com.yf.pool.threadpool.ThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yyf
 * @date 2025/8/20 0:20
 * @description
 */
@Component
public class ServiceRegistryHandler {

//    每个节点信息包含字段：ip、pid、cpuUsage（CPU 使用率）、memoryUsage（内存使用率）
//    、taskCount（任务数量）、queueCapacity(队列大小)
//    节点启动时自动向 Redis 注册，设置 35 秒过期时间（比心跳周期多5秒）
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ThreadPool threadPool;
    private final static String REGISTRY_KEY_PREFIX = "task_flow:registry";//真正的key是前缀加节点的ip和进程号
    private final static String IP;
    private final static String PID;
    private final static String KEY;
    static {
        InetAddress localHost;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        IP = localHost.getHostAddress();
        PID = String.valueOf(ProcessHandle.current().pid());
        KEY = REGISTRY_KEY_PREFIX+":"+IP+ ":"+PID;
    }
    private final static int HEART_BEAT = 30;

    @PostConstruct
    public void register(){//启动服务，向redis进行注册
        Map<String,String> map = new HashMap<>();
        map.put("ip", IP);
        map.put("pid", PID);
        map.put("cpuUsage", String.valueOf(ProcessHandle.current().info().totalCpuDuration().get()));
        map.put("memoryUsage", String.valueOf(ProcessHandle.current().info().totalCpuDuration().get()));
        map.put("taskCount",String.valueOf(threadPool.getTaskQueue().getTaskNums()));
        map.put("queueCapacity", String.valueOf(threadPool.getTaskQueue().getCapacity()));
        stringRedisTemplate.opsForHash().putAll(KEY, map);
        stringRedisTemplate.expire(KEY, HEART_BEAT+5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stringRedisTemplate.opsForHash().delete(KEY);
        }));
    }

    @Scheduled(fixedDelay = HEART_BEAT*1000)
    public void heartBeating(){//心跳机制
        if(!stringRedisTemplate.expire(KEY, HEART_BEAT+5, TimeUnit.SECONDS)){//续命失败，说明不存在key
            register();
        }
    }



}
