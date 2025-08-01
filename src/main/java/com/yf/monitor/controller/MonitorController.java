package com.yf.monitor.controller;

import com.yf.pool.entity.PoolInfo;
import com.yf.pool.threadpool.ThreadPool;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/monitor")
@AllArgsConstructor
public class MonitorController {

    private ThreadPool threadPool;

    /**
     * 获取线程池的信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo() {
        return threadPool.getThreadPoolInfo();
    }

    /**
     * 更改worker相关的参数
     */
    @PutMapping("/worker")
    public Boolean changeWorkerParams(Integer coreNums, Integer maxNums, Boolean coreDestroy, Integer aliveTime,Boolean isDaemon) {
        return threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime,isDaemon);
    }

}
