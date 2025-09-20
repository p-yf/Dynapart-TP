package com.yf.pool.rejectstrategy.impl;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.threadpool.ThreadPool;

import java.util.concurrent.FutureTask;

/**
 * @author yyf
 * @date 2025/8/2 23:24
 * @description
 */
public class DiscardStrategy extends RejectStrategy {

    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
        if(task instanceof FutureTask){
            ((FutureTask<?>) task).cancel(true);
        }
    }

}
