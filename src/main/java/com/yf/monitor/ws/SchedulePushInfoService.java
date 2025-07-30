package com.yf.monitor.ws;

import com.yf.pool.threadpool.ThreadPool;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SchedulePushInfoService {

    private ThreadPool threadPool;

    @Scheduled(fixedDelay = 1000)
    public void pushInfo() {
        ThreadPoolWebSocketHandler.broadcastThreadPoolInfo(threadPool.getThreadsInfo());
    }
}
