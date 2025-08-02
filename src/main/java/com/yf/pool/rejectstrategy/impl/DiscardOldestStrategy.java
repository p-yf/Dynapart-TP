package com.yf.pool.rejectstrategy.impl;

import com.yf.pool.rejectstrategy.RejectStrategy;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author yyf
 * @date 2025/8/2 23:13
 * @description 丢弃最老的任务或者丢弃优先级最小的任务（由队列的remove方法决定）
 */
public class DiscardOldestStrategy extends RejectStrategy {
    @Override
    public void reject(Runnable task) {
        getThreadPool().getTaskQueue().removeTask();
    }

}
