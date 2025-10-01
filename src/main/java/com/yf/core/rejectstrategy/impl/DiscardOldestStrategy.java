package com.yf.core.rejectstrategy.impl;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;

import java.util.concurrent.FutureTask;

/**
 * @author yyf
 * @date 2025/8/2 23:13
 * @description 丢弃最老的任务或者丢弃优先级最小的任务（由队列的remove方法决定）
 */
public class DiscardOldestStrategy extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
        Runnable runnable = threadPool.getPartition().removeEle();
        if(runnable instanceof FutureTask){
            ((FutureTask<?>) runnable).cancel(false);
        }
    }

}
