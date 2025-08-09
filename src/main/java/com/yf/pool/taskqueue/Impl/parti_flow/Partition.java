package com.yf.pool.taskqueue.Impl.parti_flow;

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
 * @description
 */
@Getter
@Setter
public class Partition<T>  {
    private final Lock headLock = new ReentrantLock( false);
    private final Lock tailLock = new ReentrantLock( false);
    private final Condition headCondition = headLock.newCondition();
    private final Node<T> head = new Node<>();
    private Node<T> tail = head;
    private AtomicInteger size = new AtomicInteger(0);
    private Integer capacity;

    public Partition(Integer capacity) {
        this.capacity = capacity;
    }
    public Boolean offer(T element) {
        if(element==null){
            throw new NullPointerException("元素不能为null");
        }
        Node<T> newNode = new Node<>(element);

        if(capacity==null) {
            tailLock.lock();
            try {
                tail.setNext(newNode);
                tail = newNode;
                signalWaitForElement();
                size.getAndIncrement();
            } finally {
                tailLock.unlock();
            }
        }else{
            tailLock.lock();
            try {
                if(size.get()>=capacity){
                    return false;
                }
                tail.setNext(newNode);
                tail = newNode;
                signalWaitForElement();
                size.getAndIncrement();
            } finally {
                tailLock.unlock();
            }
        }
        return true;
    }

    public T poll(Integer waitTime) throws InterruptedException {
        headLock.lock();
        try {
            while (head.getNext() == null) {
                if (waitTime != null) {
                    boolean suc = headCondition.await(waitTime, TimeUnit.MILLISECONDS);
                    if (!suc) {
                        return null;
                    }
                } else {
                    headCondition.await();
                }
            }
            Node<T> first = head.getNext();
            head.setNext(first.getNext());
            if (head.getNext() == null) {
                tail = head;
            }
            size.getAndDecrement();
            return first.getValue();
        } finally {
            headLock.unlock();
        }
    }

    public Boolean removeElement() {
        if(head.getNext() == null){
            return false;
        }
        headLock.lock();
        try{
            if(head.getNext() != null){
                head.setNext(head.getNext().getNext());
                size.getAndDecrement();
                return true;
            }else{
                return false;
            }
        }finally {
            headLock.unlock();
        }
    }

    public int getElementNums() {
        return size.get();
    }

    /**
     * 唤醒等待元素的线程
     */
    private void signalWaitForElement() {
        headLock.lock();
        try {
            headCondition.signalAll();
        } finally {
            headLock.unlock();
        }
    }
}
