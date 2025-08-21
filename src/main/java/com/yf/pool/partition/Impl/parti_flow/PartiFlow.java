package com.yf.pool.partition.Impl.parti_flow;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.partition.Impl.LinkedBlockingQPlus;
import com.yf.pool.partition.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.RemoveStrategy;
import com.yf.pool.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author yyf
 * @date 2025/8/8 17:20
 * @description
 * 分区流：多队列、细粒度、扁平化、高性能
 * 可选任务入队策略：轮询、随机、hash、填谷
 * 可选任务出队策略：轮询、随机、削峰
 * 可选移除任务策略：轮询、随机、削峰
 */
@Setter
@Getter
public class PartiFlow<T> extends Partition<T>{
    private Partition<T>[] partitions;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private OfferStrategy offerStrategy = OfferStrategy.ROUND_ROBIN;
    private PollStrategy pollStrategy = PollStrategy.ROUND_ROBIN;
    private RemoveStrategy removeStrategy = RemoveStrategy.ROUND_ROBIN;
    private AtomicInteger offerRound = new AtomicInteger(0);
    private AtomicInteger pollRound = new AtomicInteger(0);
    private AtomicInteger removeRound = new AtomicInteger(0);
    private volatile Integer capacity;
    private Integer DEFAULT_PARTITION_NUM = 5;
    private static final Integer DEFAULT_WAIT_TIME = 100;

    public PartiFlow(Integer partitionNum, Integer capacity,String QName, OfferStrategy offerStrategy, PollStrategy pollStrategy, RemoveStrategy removeStrategy) {
        this(partitionNum, capacity,QName);
        this.offerStrategy = offerStrategy;
        this.pollStrategy = pollStrategy;
        this.removeStrategy = removeStrategy;
    }

    public PartiFlow(Integer partitionNum, Integer capacity,String QName) {
        //先获取队列类型
        Class<?> qClass = OfQueue.TASK_QUEUE_MAP.get(QName);
        partitions = new Partition[partitionNum];
        this.capacity = capacity;
        if (capacity != null) {//不为null，轮询分配
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
    public PartiFlow() {
        partitions = new Partition[DEFAULT_PARTITION_NUM];
        for (int i = 0; i < DEFAULT_PARTITION_NUM; i++) {
            partitions[i] = new LinkedBlockingQPlus<>();
        }
    }


    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        int index = offerStrategy.selectPartition(partitions, element);
        Boolean suc = false;
        for(int i = 0;i<partitions.length&&!suc;i++) {
            suc = partitions[index].offer(element);
            index = (index+1)%partitions.length;
        }
            return suc;
    }

    @Override
    public T getEle(Integer waitTime) throws InterruptedException {
        T element = null;
        int partitionIndex = pollStrategy.selectPartition(partitions);
        if(waitTime==null){//说明无限等待
            int emptyCount = 0;
            while(element==null) {
                element = partitions[partitionIndex].getEle(DEFAULT_WAIT_TIME);
                if (element == null) {
                    emptyCount++;
                    //所有分区为空count清零
                    if (emptyCount >= partitions.length) {
                        emptyCount = 0;
                    }
                }
                partitionIndex = (partitionIndex+1)%partitions.length;
            }
            return element;
        }else{//说明有等待时间
            for(int i = 0;i<partitions.length&&element==null;i++) {
                element = partitions[partitionIndex].getEle(waitTime/partitions.length);
                partitionIndex = (partitionIndex+1)%partitions.length;
            }
            return element;
        }
    }

    @Override
    public Boolean removeEle() {
        return partitions[removeStrategy.selectPartition(partitions)].removeEle();
    }

    @Override
    public int getExactEleNums() {
        lockGlobally();
        try {
            return getEleNums();
        } finally {
            unlockGlobally();
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


}
