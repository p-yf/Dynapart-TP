package com.yf.pool.taskqueue;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * @author yyf
 * @description
 */
/**
 * 实现类需要保证线程安全
 */
@Getter
@Setter
public abstract class TaskQueue {
    private  final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private  final Lock rLock = rwLock.readLock();
    private  final Lock wLock = rwLock.writeLock();
    private final Condition wCondition= getWLock().newCondition();

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
    public abstract Runnable poll(Integer waitTime) throws InterruptedException;

    /**
     * 移除任务
     */
    public abstract Boolean removeTask();

    /**
     * 获取任务数量
     */
    public abstract int getExactTaskNums();//精准获取任务数量，有读锁

    /**
     * 获取任务数量
     */
    public abstract int getTaskNums();//获取任务数量，无锁
}
