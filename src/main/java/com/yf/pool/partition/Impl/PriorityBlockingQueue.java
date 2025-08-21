package com.yf.pool.partition.Impl;

import com.yf.pool.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Setter
@Getter
public class PriorityBlockingQueue<T> extends Partition<T> {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private final Lock rLock = rwLock.readLock();
    private final Lock wLock = rwLock.writeLock();
    private final Condition notEmpty = wLock.newCondition(); // 仅用于出队等待
    private volatile Integer capacity;
    private final PriorityQueue<T> q;

    public PriorityBlockingQueue(Integer capacity) {
        this.q = new PriorityQueue<>(capacity);
        this.capacity = capacity;
    }

    public PriorityBlockingQueue() {
        this.q = new PriorityQueue<>();
    }

    /**
     * 添加任务，将普通任务封装为优先级任务
     */
    public Boolean offer(T task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }
        // 第一次检查容量（带锁的精确检查在内部）
        if (capacity != null && getEleNums() >= capacity) {
            return false;
        }
        wLock.lock();
        try {
            // 二次检查容量（锁内确保准确性）
            if (capacity != null && q.size() >= capacity) {
                return false;
            }
            boolean added = q.add(task);
            notEmpty.signal(); // 唤醒等待出队的线程
            return added;
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 阻塞获取任务（保留出队等待逻辑）
     */
    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        wLock.lock();
        try {
            while (q.isEmpty()) {
                if (waitTime != null) {
                    // 限时等待
                    boolean await = notEmpty.await(waitTime, TimeUnit.MILLISECONDS);
                    if (!await) { // 超时
                        return null;
                    }
                } else {
                    notEmpty.await(); // 无限等待
                }
            }
            return q.poll(); // 获取并移除优先级最高的任务（队列头部）
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 修复：移除优先级最低的任务（原逻辑错误地移除了头部元素）
     */
    @Override
    public Boolean removeEle() {
        wLock.lock();
        try {
            if (q.isEmpty()) {
                return false; // 队列为空时直接返回失败
            }

            // 1. 遍历找到优先级最低的任务
            T lowestPriorityTask = null;
            for (T task : q) {
                if (lowestPriorityTask == null) {
                    lowestPriorityTask = task;
                    continue;
                }
                // 2. 通过比较器判断优先级（核心修复点）
                Comparator<? super T> comparator = q.comparator();
                if (comparator != null) {
                    // 若task比当前最低优先级任务更低，则更新
                    if (comparator.compare(task, lowestPriorityTask) > 0) {
                        lowestPriorityTask = task;
                    }
                } else {
                    // 若无比较器，假设T实现了Comparable（如PriorityTask）
                    @SuppressWarnings("unchecked")
                    Comparable<? super T> comparableTask = (Comparable<? super T>) task;
                    if (comparableTask.compareTo(lowestPriorityTask) > 0) {
                        lowestPriorityTask = task;
                    }
                }
            }

            // 3. 移除找到的最低优先级任务
            if (lowestPriorityTask != null) {
                q.remove(lowestPriorityTask);
                return true;
            }
            return false;
        } catch (Exception e) {
            // 处理可能的异常（如元素不存在，理论上不会发生）
            return false;
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 修复：确保size读取线程安全（与getExactEleNums保持一致）
     */
    @Override
    public int getEleNums() {
        return getExactEleNums();
    }

    @Override
    public int getExactEleNums() {
        rLock.lock();
        try {
            return q.size();
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void lockGlobally() {
        wLock.lock();
    }

    @Override
    public void unlockGlobally() {
        wLock.unlock();
    }

}
