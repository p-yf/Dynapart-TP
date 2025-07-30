package com.yf.test.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.springboot_integration.annotation.RejectStrategyBean;

@RejectStrategyBean("mys")
public class mys extends RejectStrategy {
    @Override
    public void reject(Runnable task) {

    }
}
