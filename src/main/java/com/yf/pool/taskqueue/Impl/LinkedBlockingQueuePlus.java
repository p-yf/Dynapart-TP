package com.yf.pool.taskqueue.Impl;

import com.yf.pool.taskqueue.Impl.parti_flow.Partition;
import com.yf.pool.taskqueue.TaskQueue;

/**
 * @author yyf
 * @date 2025/8/18 15:13
 * @description
 */
public class LinkedBlockingQueuePlus extends TaskQueue {
    private Partition<Runnable> q;
    public LinkedBlockingQueuePlus(Integer capacity) {
        q = new Partition<>(capacity);
    }
    @Override
    public Boolean offer(Runnable task) {
        return q.offer( task);
    }

    @Override
    public Runnable getTask(Integer waitTime) throws InterruptedException {
        return q.poll(waitTime);
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

    @Override
    public void lockGlobally() {

    }

    @Override
    public void unlockGlobally() {

    }

    @Override
    public Integer getCapacity() {
        return q.getCapacity();
    }

    @Override
    public void setCapacity(Integer capacity) {
        q.setCapacity( capacity);
    }
}
