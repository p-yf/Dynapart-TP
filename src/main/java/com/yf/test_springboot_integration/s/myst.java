package com.yf.test_springboot_integration.s;

import com.yf.pool.rejectstrategy.RejectStrategy;


/**
 * @author yyf
 * @description
 */
public class myst extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
    }

}
