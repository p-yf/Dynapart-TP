package com.yf.pool.rejectstrategy.impl;

import com.yf.pool.rejectstrategy.RejectStrategy;

public class CallerRunsStrategy extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
        System.out.println("父线程执行");
        task.run();
    }
}
