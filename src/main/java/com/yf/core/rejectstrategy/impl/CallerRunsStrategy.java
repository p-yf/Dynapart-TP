package com.yf.core.rejectstrategy.impl;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;

/**
 * @author yyf
 * @description
 */
public class CallerRunsStrategy extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
        task.run();
    }
}
