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
public class LinkedBlockingQS<T> extends Partition<T> {
    @Data
    static class Node <T>{
        private volatile T value;
        private volatile Node<T> next;

        public Node(T value) {
            this.value = value;
        }
        public Node() {
        }
    }
    private final Lock tailLock = new ReentrantLock(false);
    private final Lock headLock = new ReentrantLock(false);
    private final Condition notEmpty = headLock.newCondition();
    private final AtomicReference<Node<T>> head = new AtomicReference<>(new Node<>());
    private volatile Node<T> tail = head.get();
    private final AtomicInteger size = new AtomicInteger(0);
    private Integer capacity;

    public LinkedBlockingQS(Integer capacity) {
        this.capacity = capacity;
    }

    public LinkedBlockingQS() {
        this.capacity = null;
    }

    /**
     * 当队列有界且已满时返回false
     * 教训：能直接返回明确的结果就直接返回，否则即使逻辑正确，也有可能由于各种奇葩的原因导致错误
     * 例如下方我原本是将c设置为-1，在最后return c != -1,其实逻辑没问题，但是就是会出现问题。
     */
    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        if (capacity != null && size.get() >= capacity) {
            return false;
        }
        final int c;
        Node<T> newNode = new Node<>(element);
        tailLock.lock();
        try {
            if (capacity == null || size.get() < capacity) {
                enqueue(newNode);
                c = size.getAndIncrement();
            } else {
                return false;
            }
        } finally {
            tailLock.unlock();
        }
        // 如果添加的是第一个元素，唤醒等待的消费者
        if (c == 0) {
            signalWaitForNotEmpty();
        }
        return true;
    }


    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        long nanos = waitTime != null ? TimeUnit.MILLISECONDS.toNanos(waitTime) : 0;
        Node<T> h, first;
        int spinCount = 0;  // 限制自旋次数

        // 先尝试有限次CAS出队，减少锁竞争
        while (spinCount < 3) {  // 自旋3次后切换策略
            h = head.get();
            first = h.next;
            if (first != null) {
                // CAS替换头节点
                if (head.compareAndSet(h, first)) {
                    T result = first.value;
                    first.value = null;
                    h.next = h;
                    int c = size.getAndDecrement();
                    // 还有元素时唤醒其他消费者
                    if (c > 1) {
                        signalWaitForNotEmpty();
                    }
                    return result;
                }
                spinCount++;
                if (spinCount >= 3) {
                    Thread.yield();  // 自旋多次失败后让步，减少CPU占用
                }
            } else {
                break;  // 队列为空，退出自旋
            }
        }

        // 队列空或CAS失败次数过多，进入锁等待逻辑
        headLock.lock();
        try {
            while (true) {
                h = head.get();
                first = h.next;
                if (first != null) {
                    // 再次尝试CAS（此时竞争已减轻）
                    if (head.compareAndSet(h, first)) {
                        T result = first.value;
                        first.value = null;
                        h.next = h;
                        int c = size.getAndDecrement();
                        if (c > 1) {
                            notEmpty.signal();  // 唤醒其他等待线程
                        }
                        return result;
                    }
                    continue;  // CAS失败，重新检查
                }

                // 队列为空，处理等待
                if (waitTime != null) {
                    if (nanos <= 0) {
                        return null;  // 超时返回
                    }
                    nanos = notEmpty.awaitNanos(nanos);
                } else {
                    notEmpty.await();  // 无限等待
                }
            }
        } finally {
            headLock.unlock();
        }
    }

    @Override
    public void lockGlobally() {
        tailLock.lock();
        headLock.lock();
    }

    @Override
    public void unlockGlobally() {
        headLock.unlock();
        tailLock.unlock();
    }

    @Override
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

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
        // 注意：不再设置h.next = h，因为这会导致GC问题

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
        headLock.lock();
        try {
            notEmpty.signal();
        } finally {
            headLock.unlock();
        }
    }
}
