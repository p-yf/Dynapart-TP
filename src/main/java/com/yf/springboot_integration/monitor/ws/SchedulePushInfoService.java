package com.yf.springboot_integration.monitor.ws;

import com.yf.core.tp_regulator.UnifiedTPRegulator;
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


    /**
     *获取worker的信息
     * return Map<String, Map<Thread.State, Integer>> worker信息，String只有两个值，第一个是core，第二个是extra，state代表线程状态，Integer代表对应类别的状态的数量
     */
    @Scheduled(fixedDelayString = "${yf.thread-pool.monitor.fixedDelay}")
    public void pushInfo() {
        // 遍历所有线程池，按名称广播
        UnifiedTPRegulator.getResources().forEach((tpName, threadPool) -> {
            ThreadPoolWebSocketHandler.broadcastThreadPoolInfo(tpName, threadPool.getThreadsInfo());
            ThreadPoolWebSocketHandler.broadcastTaskNums(tpName, threadPool.getTaskNums());
            ThreadPoolWebSocketHandler.broadcastPartitionTaskNums(tpName, threadPool.getPartitionTaskNums());
        });
    }
}
