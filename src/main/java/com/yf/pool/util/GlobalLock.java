package com.yf.pool.util;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 全局锁，用来协调各个组件之间的读写
 */
public class GlobalLock {
    public static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock(true);
    public static final Lock READ_LOCK = READ_WRITE_LOCK.readLock();
    public static final Lock WRITE_LOCK = READ_WRITE_LOCK.writeLock();


    private GlobalLock(){}
}
