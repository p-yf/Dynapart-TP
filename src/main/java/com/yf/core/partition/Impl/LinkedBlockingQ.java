package com.yf.core.partition.Impl;

import com.yf.core.partition.Partition;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yyf
 * @date 2025/8/8 23:13
 * @description 优化后的分区队列，性能接近LinkedBlockingQueue
 */
@Getter
@Setter
public class LinkedBlockingQ<T> extends Partition<T> {
    @Data
    static class Node <T>{
        private T value;

        private Node<T> next;

        public Node(T value) {
            this.value =  value;
        }
        public Node() {
        }
    }

    private final Lock headLock = new ReentrantLock(false);
    private final Lock tailLock = new ReentrantLock(false);
    private final Condition notEmpty = headLock.newCondition();
    private Node<T> head = new Node<>();
    private Node<T> tail = head;
    private final AtomicInteger size = new AtomicInteger(0);
    private Integer capacity;

    public LinkedBlockingQ(Integer capacity) {
        this.capacity = capacity;
    }
    public LinkedBlockingQ() {
    }

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
        final int c;
        Node<T> newNode = new Node<>(element);
        tailLock.lock();
        try {
            // 再次检查容量，防止在获取锁前队列已被填满
            if (size.get() == capacity)
                return false;
            enqueue(newNode);
            c = size.getAndIncrement();
        } finally {
            tailLock.unlock();
        }
        // 如果队列之前为空，唤醒等待的消费者
        if (c == 0) {
            signalWaitForNotEmpty();
        }
        return true;
    }

    @Override
    public T poll(Integer waitTime) throws InterruptedException {
        T x = null;
        final int c;
        headLock.lock();
        try {
            // 队列为空时等待
            long nanos = 0;
            while (size.get() == 0) {
                if (waitTime == null) {
                    notEmpty.await();
                } else {
                    if (nanos == 0) {
                        nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
                        if (nanos <= 0) {
                            return null;
                        }
                    }
                    nanos = notEmpty.awaitNanos(nanos);
                    if (nanos <= 0) {
                        return null;
                    }
                }
            }
            x = dequeue();
            c = size.getAndDecrement();
            // 如果还有元素，唤醒其他可能等待的消费者
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            headLock.unlock();
        }
        return x;
    }


    @Override
    public void lockGlobally() {
        tailLock.lock();
        headLock.lock();
    }

    @Override
    public void unlockGlobally() {
        tailLock.unlock();
        headLock.unlock();
    }

    /**
     * 从队列获取元素，支持超时
     */


    public T removeEle() {
        T ele = null;
        // 无锁快速失败：空队列直接返回
        if (size.get() == 0) {
            return ele;
        }
        int c = -1;
        headLock.lock();
        try {
            if (size.get() > 0) {
                ele = dequeue();// 复用poll中的节点删除逻辑
                c = size.getAndDecrement();
                // 队列仍有元素：唤醒下一个消费者
                if (c > 1) {
                    notEmpty.signal();
                }
            } else {
                return null;
            }
        } finally {
            headLock.unlock();
        }
        return ele;
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
     * 从队列头部移除节点
     */
    private T dequeue() {
        Node<T> h = head;
        Node<T> first = h.getNext();
        h.setNext(h); // 帮助垃圾回收
        head = first;
        T x = first.getValue();
        first.setValue(null);
        return x;
    }

    /**
     * 唤醒等待非空条件的线程
     */
    private void signalWaitForNotEmpty() {
        headLock.lock();
        try {
            notEmpty.signal();
        } finally {
            headLock.unlock();
        }
    }
}
