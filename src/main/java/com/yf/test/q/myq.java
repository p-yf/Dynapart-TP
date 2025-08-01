package com.yf.test.q;

import com.yf.pool.springboot_integration.annotation.TaskQueueBean;
import com.yf.pool.taskqueue.TaskQueue;

@TaskQueueBean("myq")
public class myq extends TaskQueue {
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
    public int getTaskNums() {
        return 0;
    }
}
