package com.yf.pool.partition.Impl;

import com.yf.pool.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * @author yyf
 * @description
 */
/**
 * 链表结构的阻塞队列
 */
@Setter
@Getter
public class LinkedBlockingQMini<T> extends Partition<T> {//可以无界可以有界
    private  final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private  final Lock rLock = rwLock.readLock();
    private  final Lock wLock = rwLock.writeLock();
    private final Condition wCondition= getWLock().newCondition();

    private Queue<T> q;
    private volatile Integer capacity;
    public LinkedBlockingQMini(Integer capacity) {
        q = new LinkedList<>();
        this.capacity = capacity;
    }
    public LinkedBlockingQMini() {
        q = new LinkedList<>();
    }

    public void warning() {
        if(getEleNums()>10){
            System.out.println("任务数量已经超过10个!!!");
        }
    }
    /**
     * 添加任务
     */
    public Boolean offer(T task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }
        if(getCapacity()==null){//无界
            getWLock().lock(); // 获取锁
            try {
                // 添加任务到队列
                boolean added = q.add(task);
                // 唤醒等待的线程（可能有线程在poll时阻塞）
                getWCondition().signal(); // 唤醒一个等待的线程
                return added;
            } finally {
                getWLock().unlock(); // 确保锁释放
            }
        }else {//利用双重检查尽量增加性能
            if(getCapacity()>q.size()){
                getWLock().lock(); // 获取锁
                try {
                    if(getEleNums()<getCapacity()) {
                        // 添加任务到队列
                        boolean added = q.add(task);
                        // 唤醒等待的线程（可能有线程在poll时阻塞）
                        getWCondition().signal(); // 唤醒一个等待的线程
                        return added;
                    }
                    return false;
                } finally {
                    getWLock().unlock(); // 确保锁释放
                }
            }
        }
        return false;
    }

    /**
     * 获取任务
     * @param waitTime，如果为null就代表一直等待，如果不为null就代表等待aliveTime毫秒，如果等待超时则返回null
     * @return
     * @throws InterruptedException
     */
    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        getWLock().lock(); // 可中断地获取锁
        try {
            // 循环检查：避免虚假唤醒
            while (q.isEmpty()) {
                // 队列空，让当前线程阻塞等待
                if(waitTime !=null) {//表示有等待时间
                    boolean await = getWCondition().await(Long.valueOf(waitTime), TimeUnit.MILLISECONDS);// 释放锁，进入等待状态
                    if(!await) {//表示超时
                        return null;
                    }
                }else{
                    getWCondition().await();// 释放锁，进入等待状态
                }
            }
            // 队列有任务，取出并返回
            return q.poll();
        } finally {
            getWLock().unlock(); // 确保锁释放
        }
    }

    /**
     * 移除最老的任务
     * @return
     */
    @Override
    public Boolean removeEle() {
        if (q.isEmpty()) {
            return false;
        }
        getWLock().lock();
        try {
            if(!q.isEmpty()) {
                q.remove();
                return true;
            }else{
                return false;
            }
        }finally {
            getWLock().unlock();
        }
    }

    @Override
    public int getExactEleNums() {
        getRLock().lock();
        try {
            return q.size();
        } finally {
            getRLock().unlock();
        }
    }

    @Override
    public int getEleNums() {
        return q.size();
    }

    @Override
    public void lockGlobally() {
        wLock.lock();
    }

    @Override
    public void unlockGlobally() {
        wLock.unlock();
    }

    @Override
    public Integer getCapacity() {
        return capacity;
    }

    @Override
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }



}
