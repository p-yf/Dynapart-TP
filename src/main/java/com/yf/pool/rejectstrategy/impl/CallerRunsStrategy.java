package com.yf.pool.rejectstrategy.impl;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.threadpool.ThreadPool;

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
