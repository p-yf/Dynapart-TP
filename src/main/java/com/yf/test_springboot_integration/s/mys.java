package com.yf.test_springboot_integration.s;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.threadpool.ThreadPool;
import com.yf.springboot_integration.pool.annotation.RSResource;

/**
 * @author yyf
 * @description
 */
@RSResource("mys")
public class mys extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
    }

}
