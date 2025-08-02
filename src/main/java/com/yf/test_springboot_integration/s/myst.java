package com.yf.test_springboot_integration.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.springboot_integration.annotation.RejectStrategyBean;


/**
 * @author yyf
 * @description
 */
@RejectStrategyBean("myst")
public class myst extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
    }

}
