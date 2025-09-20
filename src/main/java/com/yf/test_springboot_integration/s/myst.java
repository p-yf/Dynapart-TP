package com.yf.test_springboot_integration.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.threadpool.ThreadPool;


/**
 * @author yyf
 * @description
 */
public class myst extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
    }

}
