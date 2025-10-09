package com.yf.test_or_explore_springboot_integration.test_resource.s;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;
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
