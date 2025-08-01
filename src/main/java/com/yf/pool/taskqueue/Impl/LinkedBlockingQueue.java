package com.yf.pool.taskqueue.Impl;

import com.yf.pool.taskqueue.TaskQueue;
import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * 链表结构的阻塞队列
 */
@Data
public class LinkedBlockingQueue extends TaskQueue {//可以无界可以有界
    private Queue<Runnable> queue = new LinkedList<>();

    public LinkedBlockingQueue(Integer capacity) {
        setCapacity(capacity);
    }

    /**
     * 添加任务
     * @param task
     * @return
     */
    public Boolean addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }
        getWLock().lock(); // 获取锁
        try {
            // 添加任务到队列
            boolean added = queue.add(task);
            // 唤醒等待的线程（可能有线程在poll时阻塞）
            getWCondition().signal(); // 唤醒一个等待的线程
            return added;
        } finally {
            getWLock().unlock(); // 确保锁释放
        }
    }

    /**
     * 获取任务
     * @param waitTime，如果为null就代表一直等待，如果不为null就代表等待aliveTime毫秒，如果等待超时则返回null
     * @return
     * @throws InterruptedException
     */
    @Override
    public Runnable poll(Integer waitTime) throws InterruptedException {
        getWLock().lock(); // 可中断地获取锁
        try {
            // 循环检查：避免虚假唤醒（spurious wakeup）
            while (queue.isEmpty()) {
                // 队列空，让当前线程阻塞等待
                if(waitTime !=null) {//表示有等待时间
                    boolean await = getWCondition().await(Long.valueOf(waitTime), TimeUnit.SECONDS);// 释放锁，进入等待状态
                    if(!await) {//表示超时
                        return null;
                    }
                }else{
                    getWCondition().await();// 释放锁，进入等待状态
                }
            }
            // 队列有任务，取出并返回
            return queue.poll();
        } finally {
            getWLock().unlock(); // 确保锁释放
        }
    }

    /**
     * 移除最老的任务
     * @return
     */
    @Override
    public Boolean removeTask() {
        getWLock().lock();
        try {
            if (queue.isEmpty()) {
            } else {
                queue.remove();
            }
        }finally {
            getWLock().unlock();
        }
        return true;
    }

    @Override
    public int getTaskNums() {
        getRLock().lock();
        try {
            return queue.size();
        } finally {
            getRLock().unlock();
        }
    }
}
