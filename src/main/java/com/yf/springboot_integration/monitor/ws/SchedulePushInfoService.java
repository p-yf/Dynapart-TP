package com.yf.springboot_integration.monitor.ws;

import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;


/**
 * @author yyf
 * @description
 */
@Slf4j
public class SchedulePushInfoService {


    /**
     *获取worker的信息
     * return Map<String, Map<Thread.State, Integer>> worker信息，String只有两个值，第一个是core，第二个是extra，state代表线程状态，Integer代表对应类别的状态的数量
     */
    @Scheduled(fixedDelayString = "${yf.thread-pool.monitor.fixedDelay}")
    public void pushInfo() {
        try {
            // 遍历所有线程池，按名称广播
            UnifiedTPRegulator.getResources().forEach((tpName, threadPool) -> {
                if (threadPool == null) {
                    log.warn("Skipping null thread pool for name: {}", tpName);
                    return;
                }
                try {
                    log.debug("Pushing info for thread pool: {}", tpName);
                    ThreadPoolWebSocketHandler.broadcastThreadPoolInfo(tpName, threadPool.getThreadsInfo());
                    ThreadPoolWebSocketHandler.broadcastTaskNums(tpName, threadPool.getTaskNums());
                    ThreadPoolWebSocketHandler.broadcastPartitionTaskNums(tpName, threadPool.getPartitionTaskNums());
                } catch (Exception e) {
                    log.error("Failed to push info for thread pool [{}]: {}", tpName, e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("Error in scheduled pushInfo task: {}", e.getMessage(), e);
        }
    }
}
