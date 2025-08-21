package com.yf.pool.partition.Impl;

import com.yf.pool.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yyf
 * @date 2025/8/8 23:13
 * @description 优化后的分区队列，出队操作使用CAS实现
 */
@Getter
@Setter
public class LinkedBlockingQPro<T> extends Partition<T> {
    private final Lock tailLock = new ReentrantLock(false);
    private final Lock conditionLock = new ReentrantLock(false);
    private final Condition notEmpty = conditionLock.newCondition();
    private final AtomicReference<Node<T>> head = new AtomicReference<>(new Node<>());
    private Node<T> tail = head.get();
    private final AtomicInteger size = new AtomicInteger(0);
    private Integer capacity;

    public LinkedBlockingQPro(Integer capacity) {
        this.capacity = capacity;
    }

    public LinkedBlockingQPro() {}

    /**
     * 当队列有界且已满时返回false
     */
    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        // 有界队列且已满时直接返回false
        if (capacity != null && size.get() == capacity) {
            return false;
        }
        int c = -1;
        Node<T> newNode = new Node<>(element);
        tailLock.lock();
        try {
            // 再次检查容量，防止在获取锁前队列已被填满
            if (capacity == null || size.get() < capacity) {
                enqueue(newNode);
                c = size.getAndIncrement();//先返回当前返回值，再加1
            }
        } finally {
            tailLock.unlock();
        }
        // 如果队列之前为空，唤醒等待的消费者
        if (c == 0) {
            signalWaitForNotEmpty();
        }
        return c != -1;
    }

    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        T x = null;
        int c = -1;
        long nanos = 0;
        long start = System.nanoTime();
        // 将等待和CAS操作合并到一个循环中，解决竞态条件
        while (true) {
            // 检查队列是否有元素
            if (size.get() == 0) {
                // 队列为空时等待
                if (waitTime == null) {
                    conditionLock.lock();
                    try {
                        notEmpty.await();
                    } finally {
                        conditionLock.unlock();
                    }
                } else {
                    if (nanos == 0) {
                        nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
                        if (nanos <= 0) {
                            return null;
                        }
                    }
                    conditionLock.lock();
                    try {
                        nanos = notEmpty.awaitNanos(nanos);
                        nanos -= System.nanoTime() - start;
                    } finally {
                        conditionLock.unlock();
                    }
                    if (nanos <= 0) {
                        return null;
                    }
                }
                // 等待后继续循环检查
                continue;
            }

            // 使用CAS操作出队
            Node<T> h = head.get();
            Node<T> first = h.getNext();

            // 双重检查：确保队列确实有元素
            if (first == null) {
                continue;
            }

            // 尝试CAS更新头节点
            if (head.compareAndSet(h, first)) {
                // CAS成功，完成出队操作
                x = first.getValue();
                first.setValue(null); // 帮助GC
                h.setNext(h); // 原头节点自引用，帮助GC

                c = size.getAndDecrement();
                // 如果还有元素，唤醒其他可能等待的消费者
                if (c > 1) {
                    signalWaitForNotEmpty();
                }
                break;
            }
        }

        return x;
    }



    @Override
    public void lockGlobally() {
        tailLock.lock();
        conditionLock.lock();
    }

    @Override
    public void unlockGlobally() {
        conditionLock.unlock();
        tailLock.unlock();
    }

    /**
     * 从队列获取元素，支持超时
     */


    public Boolean removeEle() {
        // 无锁快速失败：空队列直接返回
        if (size.get() == 0) {
            return false;
        }

        int c = -1;
        // 使用CAS操作出队
        Node<T> h, first;
        do {
            h = head.get();
            first = h.getNext();
            // 如果没有元素，返回失败
            if (first == null) {
                return false;
            }
        } while (!head.compareAndSet(h, first));

        first.setValue(null); // 帮助GC
        h.setNext(h); // 原头节点自引用，帮助GC

        c = size.getAndDecrement();
        // 队列仍有元素：唤醒下一个消费者
        if (c > 1) {
            signalWaitForNotEmpty();
        }

        return true;
    }

    public int getEleNums() {
        return size.get();
    }

    /**
     * 将节点加入队列尾部
     */
    private void enqueue(Node<T> node) {
        tail.setNext(node);
        tail = node;
    }

    /**
     * 唤醒等待非空条件的线程
     */
    private void signalWaitForNotEmpty() {
        conditionLock.lock();
        try {
            notEmpty.signal();
        } finally {
            conditionLock.unlock();
        }
    }
}
