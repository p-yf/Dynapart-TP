package com.yf.pool.taskqueue.Impl;

import com.yf.pool.task.PriorityTask;
import com.yf.pool.taskqueue.TaskQueue;
import lombok.Getter;
import lombok.Setter;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author yyf
 * @date 2025/8/2 14:09
 * @description:使用优先级队列的时候，提交任务可以直接用普通任务，默认优先级为0最小。也可以用优先级任务，优先级任务需要将普通任务封装进优先级任务，并且指定优先级
 */
@Setter
@Getter
public class PriorityBlockingQueue extends TaskQueue {

    private PriorityQueue<PriorityTask> q;
    public PriorityBlockingQueue(Integer capacity) {
        q = new PriorityQueue<>();
        setCapacity(capacity);
    }

    /**
     * 添加任务
     * @param task
     * @return
     */
    public Boolean addTask(Runnable task) {
        if(task instanceof PriorityTask) {
            PriorityTask priorityTask = (PriorityTask) task;
            return addTask(priorityTask);
        }else{
            PriorityTask priorityTask = new PriorityTask(task, null,0);
            return addTask(priorityTask);
        }
    }
    private Boolean addTask(PriorityTask task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }
        if (getCapacity() != null && getCapacity() > q.size()) {//利用双重检查尽量增加性能
            getWLock().lock(); // 获取锁
            try {
                if (getTaskNums() < getCapacity()) {
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
        return false;
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
            // 循环检查：避免虚假唤醒
            while (q.isEmpty()) {
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
            return q.poll();
        } finally {
            getWLock().unlock(); // 确保锁释放
        }
    }

    /**
     * 移除优先级最低的任务
     * @return
     */
    @Override
    public Boolean removeTask() {
        getWLock().lock();
        try {
            if (q.isEmpty()) {
            } else {
                q.remove();
            }
        }finally {
            getWLock().unlock();
        }
        return true;
    }

    @Override
    public int getExactTaskNums() {
        getRLock().lock();
        try {
            return q.size();
        } finally {
            getRLock().unlock();
        }
    }

    @Override
    public int getTaskNums() {
        return q.size();
    }
}
