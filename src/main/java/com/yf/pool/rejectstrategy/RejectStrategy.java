package com.yf.pool.rejectstrategy;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * @author yyf
 * @description
 */
@Getter
@Setter
public abstract class  RejectStrategy {
    public final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    public final Lock rLock = rwLock.readLock();
    public  final Lock wLock = rwLock.writeLock();
    private final Condition wCondition= getWLock().newCondition();

    private ThreadPool threadPool;
    public abstract void reject (Runnable task);//处理普通任务的

}
