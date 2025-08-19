package com.yf.pool.taskqueue.Impl;

import com.yf.pool.task.PriorityTask;
import com.yf.pool.taskqueue.TaskQueue;
import lombok.Getter;
import lombok.Setter;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Setter
@Getter
public class PriorityBlockingQueue extends TaskQueue {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private final Lock rLock = rwLock.readLock();
    private final Lock wLock = rwLock.writeLock();
    private final Condition notEmpty = wLock.newCondition(); // 仅用于出队等待
    private volatile Integer capacity;
    private final PriorityQueue<PriorityTask> q;

    public PriorityBlockingQueue(Integer capacity) {
        this.q = new PriorityQueue<>();
        this.capacity = capacity;
    }

    /**
     * 添加任务，将普通任务封装为优先级任务
     */
    public Boolean offer(Runnable task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }

        if (task instanceof PriorityTask) {
            return offer((PriorityTask) task);
        } else {
            PriorityTask priorityTask = new PriorityTask(task, null, 0);
            return offer(priorityTask);
        }
    }

    /**
     * 添加优先级任务
     */
    private boolean offer(PriorityTask task) {
        if (task == null) {
            throw new NullPointerException("任务不能为null");
        }
        if (capacity != null && getTaskNums() >= capacity) {
            return false;
        }
        wLock.lock();
        try {
            if (capacity != null && q.size() >= capacity) {
                return false;
            }
            boolean added = q.add(task);
            // 唤醒等待出队的线程
            notEmpty.signal();
            return added;
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 阻塞获取任务（保留出队等待逻辑）
     */
    @Override
    public Runnable getTask(Integer waitTime) throws InterruptedException {
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
                    notEmpty.await();
                }
            }
            return q.poll();
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 移除优先级最低的任务
     */
    @Override
    public Boolean removeTask() {
        wLock.lock();
        try {
            if (q.isEmpty()) {
                return false;
            }
            // 找到优先级最低的任务
            PriorityTask lowest = null;
            for (PriorityTask task : q) {
                if (lowest == null || task.getPriority() < lowest.getPriority()) {
                    lowest = task;
                }
            }

            if (lowest != null) {
                q.remove(lowest);
                return true;
            }
            return false;
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public int getExactTaskNums() {
        rLock.lock();
        try {
            return q.size();
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public int getTaskNums() {
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
}
