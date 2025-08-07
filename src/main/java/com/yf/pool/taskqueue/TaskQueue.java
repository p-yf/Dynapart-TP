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
    public Boolean addTask(Runnable task){
        warning();
        return offer(task);
    };

    /**
     * 添加任务的方法
     * @return
     */
    public abstract Boolean offer(Runnable task);

    /**
     * 警告
     */
    public void warning(){};

    /**
     * 获取任务
      * @return
     */
    public abstract Runnable getTask(Integer waitTime) throws InterruptedException;

    /**
     * 移除任务(用于丢弃策略)
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
