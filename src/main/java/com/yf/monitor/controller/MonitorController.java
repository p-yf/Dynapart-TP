package com.yf.monitor.controller;

import com.yf.pool.entity.PoolInfo;
import com.yf.pool.threadpool.ThreadPool;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/monitor")
@AllArgsConstructor
public class MonitorController {

    private ThreadPool threadPool;
    /**
     * 获取线程的信息
     */
    @GetMapping("/threads")
    public Map<String, Map<Thread.State,Integer>> getThreadsInfo() {
        return threadPool.getThreadsInfo();
    }

    /**
     * 获取线程池的信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo() {
        return threadPool.getThreadPoolInfo();
    }

    /**
     * 更改线程池的配置
     */

}
