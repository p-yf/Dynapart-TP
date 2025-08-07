package com.yf.test_springboot_integration.q;

import com.yf.springboot_integration.pool.annotation.TaskQueueBean;
import com.yf.pool.taskqueue.TaskQueue;

/**
 * @author yyf
 * @description
 */
@TaskQueueBean("myqn")
public class myqn extends TaskQueue {
    @Override
    public Boolean offer(Runnable task) {
        return null;
    }

    @Override
    public Runnable getTask(Integer waitTime) throws InterruptedException {
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
