package com.yf.pool.partition.Impl;

import com.yf.pool.partition.Partition;
import lombok.Data;
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
    @Data
    static class Node <T>{
        private volatile T value;

        private volatile Node<T> next;

        public Node(T value) {
            this.value =  value;
        }
        public Node() {
        }
    }
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
        if (capacity != null && size.get() >= capacity) {
            return false;
        }

        Node<T> newNode = new Node<>(element);
        tailLock.lock();
        try {
            // 再次检查容量，防止在获取锁前队列已被填满
            if (capacity != null && size.get() >= capacity) {
                return false;
            }

            enqueue(newNode);
            int oldSize = size.getAndIncrement();

            // 如果队列之前为空，唤醒等待的消费者
            if (oldSize == 0) {
                signalWaitForNotEmpty();
            }
            return true;
        } finally {
            tailLock.unlock();
        }
    }

    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        T x = null;
        long nanos = 0;
        if (waitTime != null) {
            nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
        }

        conditionLock.lock();
        try {
            // 等待直到队列不为空或超时
            while (size.get() == 0) {
                if (waitTime == null) {
                    notEmpty.await();
                } else {
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = notEmpty.awaitNanos(nanos);
                    if (nanos <= 0) {
                        return null;
                    }
                }
            }
        } finally {
            conditionLock.unlock();
        }

        // 尝试出队操作
        while (true) {
            Node<T> h = head.get();
            Node<T> first = h.next;

            // 再次检查队列是否还有元素
            if (first == null) {
                // 队列变空了，重新等待
                conditionLock.lock();
                try {
                    if (size.get() == 0) {
                        notEmpty.await();
                    }
                } finally {
                    conditionLock.unlock();
                }
                continue;
            }

            // 尝试CAS更新头节点
            if (head.compareAndSet(h, first)) {
                x = first.value;
                first.value = null; // 帮助GC

                int oldSize = size.getAndDecrement();
                // 如果还有元素，唤醒其他可能等待的消费者
                if (oldSize > 1) {
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
