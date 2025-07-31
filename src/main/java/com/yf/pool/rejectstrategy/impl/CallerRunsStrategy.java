package com.yf.pool.rejectstrategy.impl;

import com.yf.pool.rejectstrategy.RejectStrategy;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CallerRunsStrategy extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
        System.out.println("父线程执行");
        task.run();
    }

    @Override
    public Future reject(FutureTask task) {
        System.out.println("父线程执行");
        task.run();
        return task;
    }
}
