package com.yf.core.partitioning.impl;

import com.yf.common.exception.SwitchedException;
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partitioning.Partitioning;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.RoundRobinOffer;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.RoundRobinPoll;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.RoundRobinRemove;
import com.yf.core.partition.Partition;
import com.yf.core.resource_manager.PartiResourceManager;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;

/**
 * @author yyf
 * @date 2025/9/23 16:02
 * @description
 *
 *《静态分区队列》
 * 这是一款不支持调度规则执行后轮询的分区队列，无论调度策略是否设置支持轮询都没用，因而结尾为Still，代表着静态
 * 也正是由于这个特性，他的性能会比PartiFlow更好一些。
 */
@Getter
@Setter
public class PartiStill<T> extends Partition<T> implements Partitioning<T> {
    private Partition<T>[] partitions;
    private OfferPolicy offerPolicy = new RoundRobinOffer();
    private PollPolicy pollPolicy = new RoundRobinPoll();
    private RemovePolicy removePolicy = new RoundRobinRemove();
    private volatile Integer capacity;
    private Integer DEFAULT_PARTITION_NUM = 5;

    public PartiStill(Integer partitionNum, Integer capacity, String QName, OfferPolicy offerPolicy, PollPolicy pollPolicy, RemovePolicy removePolicy) {
        this(partitionNum, capacity,QName);
        this.offerPolicy = offerPolicy;
        this.pollPolicy = pollPolicy;
        this.removePolicy = removePolicy;
    }

    public PartiStill(Integer partitionNum, Integer capacity, String QName) {
        //先获取队列类型
        Class<?> qClass = PartiResourceManager.getResources().get(QName);
        partitions = new Partition[partitionNum];
        this.capacity = capacity;
        if (capacity != null) {//不为null，轮询分配容量
            final int baseCapacity = capacity / partitionNum;
            final int remainder = capacity % partitionNum;
            for (int i = 0; i < partitionNum; i++) {
                int partitionCapacity = baseCapacity + (i < remainder ? 1 : 0);
                try {
                    partitions[i] = (Partition<T>) qClass.getConstructor().newInstance();
                    partitions[i].setCapacity(partitionCapacity);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {//为null，都为无界队列
            for (int i = 0; i < partitionNum; i++) {
                try {
                    partitions[i] = (Partition<T>) qClass.getConstructor().newInstance();
                    partitions[i].setCapacity(null);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public PartiStill() {
        partitions = new Partition[DEFAULT_PARTITION_NUM];
        for (int i = 0; i < DEFAULT_PARTITION_NUM; i++) {
            partitions[i] = new LinkedBlockingQ<>();
        }
    }


    public boolean offer(T element) {
        try {
            return partitions[offerPolicy.selectPartition(partitions, element)].offer(element);
        } catch (SwitchedException e) {
            return false;
        }
    }

    @Override
    public T poll(Integer waitTime) throws InterruptedException {
        try {
            return partitions[pollPolicy.selectPartition(partitions)].poll(waitTime);
        } catch (SwitchedException e) {
            return null;
        }
    }
    @Override
    public T removeEle() {
        try {
            return partitions[removePolicy.selectPartition(partitions)].removeEle();
        } catch (SwitchedException e) {
            return null;
        }
    }


    @Override
    public int getEleNums() {
        int sum = 0;
        for (Partition partition : partitions) {
            sum += partition.getEleNums();
        }
        return sum;
    }

    public void lockGlobally(){
        for(Partition partition : partitions) {
            partition.lockGlobally();
        }
    }

    public void unlockGlobally(){
        for(Partition partition : partitions) {
            partition.unlockGlobally();
        }
    }

    @Override
    public void markAsSwitched() {
        for(Partition partition : partitions) {
            partition.markAsSwitched();
        }
    }
}
