package com.yf.monitor.ws;

import com.yf.pool.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class SchedulePushInfoService {

    private ThreadPool threadPool;
    public SchedulePushInfoService(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Scheduled(fixedDelayString = "${fy.thread-pool.monitor.fixedDelay}")
    public void pushInfo() {
        Map<String, Map<Thread.State, Integer>> threadsInfo = threadPool.getThreadsInfo();
        ThreadPoolWebSocketHandler.broadcastThreadPoolInfo(threadsInfo);
    }
}
