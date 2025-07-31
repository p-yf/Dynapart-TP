package com.yf.pool.taskqueue;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

/**
 * 实现类需要保证线程安全
 */
@Getter
@Setter
public abstract class TaskQueue {

    private Integer capacity;

    private ThreadPool threadPool;
    /**
     * 添加任务
     * @param task
     * @return
     */
    public abstract Boolean addTask(Runnable task);

    /**
     * 获取任务
      * @return
     */
    public abstract Runnable poll(Integer aliveTime) throws InterruptedException;

    /**
     * 移除任务
     */
    public abstract Boolean removeTask();

    /**
     * 获取长度
     */
    public abstract int getSize();
}
