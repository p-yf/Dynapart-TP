package com.yf.pool.partition.Impl.parti_flow;

import com.yf.pool.constant_or_registry.QueueManager;
import com.yf.pool.partition.Impl.LinkedBlockingQ;
import com.yf.pool.partition.Impl.parti_flow.strategy.OfferPolicy;
import com.yf.pool.partition.Impl.parti_flow.strategy.PollPolicy;
import com.yf.pool.partition.Impl.parti_flow.strategy.RemovePolicy;
import com.yf.pool.partition.Impl.parti_flow.strategy.impl.offer_policy.RoundRobinOffer;
import com.yf.pool.partition.Impl.parti_flow.strategy.impl.poll_policy.RoundRobinPoll;
import com.yf.pool.partition.Impl.parti_flow.strategy.impl.poll_policy.ThreadBindingPoll;
import com.yf.pool.partition.Impl.parti_flow.strategy.impl.remove_policy.RoundRobinRemove;
import com.yf.pool.partition.Partition;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/8 17:20
 * @description
 * 分区流：多队列、细粒度、扁平化、高性能
 * 可选任务入队策略：轮询、随机、hash、填谷
 * 可选任务出队策略：轮询、随机、削峰、线程绑定
 * 可选移除任务策略：轮询、随机、削峰
 */
@Setter
@Getter
public class PartiFlow<T> extends Partition<T>{
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

    public PartiFlow(Integer partitionNum, Integer capacity, String QName, OfferPolicy offerPolicy, PollPolicy pollPolicy, RemovePolicy removePolicy) {
        this(partitionNum, capacity,QName);
        this.offerPolicy = offerPolicy;
        this.pollPolicy = pollPolicy;
        this.removePolicy = removePolicy;
    }

    public PartiFlow(Integer partitionNum, Integer capacity,String QName) {
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
    public PartiFlow() {
        partitions = new Partition[DEFAULT_PARTITION_NUM];
        for (int i = 0; i < DEFAULT_PARTITION_NUM; i++) {
            partitions[i] = new LinkedBlockingQ<>();
        }
    }


    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        int index = offerPolicy.selectPartition(partitions, element);
        Boolean suc = false;
        for(int i = 0;i<partitions.length&&!suc;i++) {
            suc = partitions[index].offer(element);
            index = (index+1)%partitions.length;
        }
            return suc;
    }

    @Override
    public T poll(Integer waitTime) throws InterruptedException {
        if(pollPolicy instanceof ThreadBindingPoll){//线程绑定不轮询
            return partitions[pollPolicy.selectPartition( partitions)].poll(waitTime);
        }
        T element = null;
        int partitionIndex = pollPolicy.selectPartition(partitions);
        if(waitTime==null){//说明无限等待
            int emptyCount = 0;
            while(element==null) {
                element = partitions[partitionIndex].poll(DEFAULT_WAIT_TIME);
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
                element = partitions[partitionIndex].poll(waitTime/partitions.length);
                partitionIndex = (partitionIndex+1)%partitions.length;
            }
            return element;
        }
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
