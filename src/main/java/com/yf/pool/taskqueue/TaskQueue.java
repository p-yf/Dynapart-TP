package com.yf.pool.taskqueue;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
public abstract class TaskQueue {

    private Integer capacity;
    private Integer aliveTime;
    // 底层用LinkedList存储任务
    private Queue<Runnable> queue = new LinkedList<>();
    // 锁：保证线程安全
    private Lock lock = new ReentrantLock();
    // 条件变量：用于线程的阻塞和唤醒
    private Condition notEmpty = lock.newCondition();
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
}
