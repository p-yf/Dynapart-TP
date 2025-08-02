package com.yf.pool.taskqueue.Impl;

import com.yf.pool.taskqueue.TaskQueue;

/**
 * @author yyf
 * @date 2025/8/2 14:09
 * @description
 */
public class PriorityBlockingQueue extends TaskQueue {
    @Override
    public Boolean addTask(Runnable task) {
        return null;
    }

    @Override
    public Runnable poll(Integer waitTime) throws InterruptedException {
        return null;
    }

    @Override
    public Boolean removeTask() {
        return null;
    }

    @Override
    public int getExactTaskNums() {
        return 0;
    }

    @Override
    public int getTaskNums() {
        return 0;
    }
}
