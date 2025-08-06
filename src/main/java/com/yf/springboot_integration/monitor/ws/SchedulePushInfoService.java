package com.yf.springboot_integration.monitor.ws;

import com.yf.pool.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


/**
 * @author yyf
 * @description
 */
@Slf4j
@Service
public class SchedulePushInfoService {

    private ThreadPool threadPool;
    public SchedulePushInfoService(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }


    /**
     *获取worker的信息
     * return Map<String, Map<Thread.State, Integer>> worker信息，String只有两个值，第一个是core，第二个是extra，state代表线程状态，Integer代表对应类别的状态的数量
     */
    @Scheduled(fixedDelayString = "${yf.thread-pool.monitor.fixedDelay}")
    public void pushInfo() {
        ThreadPoolWebSocketHandler.broadcastThreadPoolInfo(threadPool.getThreadsInfo());
        ThreadPoolWebSocketHandler.broadcastTaskNums(threadPool.getTaskNums());
    }
}
