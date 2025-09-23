package com.yf.test_springboot_integration.s;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;


/**
 * @author yyf
 * @description
 */
public class myst extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
    }

}
