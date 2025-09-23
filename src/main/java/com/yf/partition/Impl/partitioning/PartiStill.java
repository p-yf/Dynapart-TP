package com.yf.partition.Impl.partitioning;

import com.yf.partition.Impl.LinkedBlockingQ;
import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.RoundRobinOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.RoundRobinPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.remove_policy.RoundRobinRemove;
import com.yf.partition.Partition;
import com.yf.pool.constant_or_registry.QueueManager;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/23 16:02
 * @description
 */
@Getter
@Setter
public class PartiStill<T> extends Partition<T> {
    private Partition<T>[] partitions;
    private OfferPolicy offerPolicy = new RoundRobinOffer();
    private PollPolicy pollPolicy = new RoundRobinPoll();
    private RemovePolicy removePolicy = new RoundRobinRemove();
    private AtomicInteger offerRound = new AtomicInteger(0);
    private AtomicInteger pollRound = new AtomicInteger(0);
    private AtomicInteger removeRound = new AtomicInteger(0);
    private volatile Integer capacity;
    private Integer DEFAULT_PARTITION_NUM = 5;
    private static final Integer DEFAULT_WAIT_TIME = 100;

    public PartiStill(Integer partitionNum, Integer capacity, String QName, OfferPolicy offerPolicy, PollPolicy pollPolicy, RemovePolicy removePolicy) {
        this(partitionNum, capacity,QName);
        this.offerPolicy = offerPolicy;
        this.pollPolicy = pollPolicy;
        this.removePolicy = removePolicy;
    }

    public PartiStill(Integer partitionNum, Integer capacity,String QName) {
        //先获取队列类型
        Class<?> qClass = QueueManager.getResources().get(QName);
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
    public PartiStill() {
        partitions = new Partition[DEFAULT_PARTITION_NUM];
        for (int i = 0; i < DEFAULT_PARTITION_NUM; i++) {
            partitions[i] = new LinkedBlockingQ<>();
        }
    }


    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        return partitions[offerPolicy.selectPartition(partitions, element)].offer(element);
    }

    @Override
    public T poll(Integer waitTime) throws InterruptedException {
        return partitions[pollPolicy.selectPartition( partitions)].poll(waitTime);
    }
    @Override
    public Boolean removeEle() {
        return partitions[removePolicy.selectPartition(partitions)].removeEle();
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
