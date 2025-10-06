package com.yf.common.task.impl;

import com.yf.common.task.GCTask;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.worker.Worker;

import java.util.Set;

/**
 * @author yyf
 * @date 2025/10/6 15:47
 * @description
 */
public class TBPollCleaningTask extends GCTask {
    @Override
    public void run() {//队列切换后所做的事情：销毁所有线程
        ThreadPool threadPool = getThreadPool();
        Set<Worker> coreList = threadPool.getCoreList();
        Set<Worker> extraList = threadPool.getExtraList();
        UnifiedTPRegulator.destroyWorkers(threadPool.getName(),coreList.size(),extraList.size());
    }

    @Override
    public GCTask build(ThreadPool tp) {
        setThreadPool(tp);
        return this;
    }
}
