package com.yf.core.partition.Impl;

import com.yf.common.exception.SwitchedException;
import com.yf.core.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

@Setter
@Getter
public class PriorityBlockingQ<T> extends Partition<T> {
    private final Lock lock = new ReentrantLock( false);
    private final Condition notEmpty = lock.newCondition(); // 仅用于出队等待
    private volatile Integer capacity;
    private final PriorityQueue<T> q;
    private boolean switched = false;

    public PriorityBlockingQ(Integer capacity) {
        this.q = new PriorityQueue<>(capacity);
        this.capacity = capacity;
    }

    public PriorityBlockingQ() {
        this.q = new PriorityQueue<>();
    }

    /**
     * 添加任务，将普通任务封装为优先级任务
     */
    public boolean offer(T task) {
        if (task == null) {
            throw new NullPointerException("元素不能为null");
        }
        // 第一次检查容量（带锁的精确检查在内部）
        if (capacity != null && getEleNums() >= capacity) {
            return false;
        }
        lock.lock();
        try {
            if(switched){
                throw new SwitchedException();
            }
            // 二次检查容量（锁内确保准确性）
            if (capacity != null && q.size() >= capacity) {
                return false;
            }
            boolean added = q.add(task);
            notEmpty.signal(); // 唤醒等待出队的线程
            return added;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞获取任务（保留出队等待逻辑）
     */
    @Override
    public T poll(Integer waitTime) throws InterruptedException {
        lock.lock();
        try {
            if(switched){
                throw new SwitchedException();
            }
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
            lock.unlock();
        }
    }

    @Override
    public T removeEle() {
        lock.lock();
        try{
            if(switched){
                throw new SwitchedException();
            }
            if(q.isEmpty()){
                return null;
            }else{
                return q.remove();
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public int getEleNums() {
        return q.size();
    }



    @Override
    public void lockGlobally() {
        lock.lock();
    }

    @Override
    public void unlockGlobally() {
        lock.unlock();
    }

    @Override
    public void markAsSwitched() {
        switched = true;
    }

}
