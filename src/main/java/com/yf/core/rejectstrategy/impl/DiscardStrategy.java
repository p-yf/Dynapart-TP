package com.yf.core.rejectstrategy.impl;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;

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
