package com.yf.test.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.springboot_integration.annotation.RejectStrategyBean;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@RejectStrategyBean("myst")
public class myst extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
    }

    @Override
    public Future reject(FutureTask task) {
        return null;
    }
}
