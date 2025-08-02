package com.yf.test_springboot_integration.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.springboot_integration.annotation.RejectStrategyBean;

/**
 * @author yyf
 * @description
 */
@RejectStrategyBean("mys")
public class mys extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
    }

}
