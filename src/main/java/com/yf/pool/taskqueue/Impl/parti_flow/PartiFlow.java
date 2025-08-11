package com.yf.pool.taskqueue.Impl.parti_flow;

import com.yf.pool.taskqueue.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.RemoveStrategy;
import lombok.Getter;
import lombok.Setter;

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
 * 移除任务策略：轮询、随机、削峰
 */
@Setter
@Getter
public class PartiFlow<T> {
    private Partition<T>[] partitions;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private OfferStrategy offerStrategy = OfferStrategy.ROUND_ROBIN;
    private PollStrategy pollStrategy = PollStrategy.ROUND_ROBIN;
    private RemoveStrategy removeStrategy = RemoveStrategy.ROUND_ROBIN;
    private AtomicInteger offerRound = new AtomicInteger(0);
    private AtomicInteger pollRound = new AtomicInteger(0);
    private AtomicInteger removeRound = new AtomicInteger(0);
    private Integer capacity;
    private static final Integer DEFAULT_WAIT_TIME = 100;

    public PartiFlow(Integer partitionNum, Integer capacity, OfferStrategy offerStrategy, PollStrategy pollStrategy, RemoveStrategy removeStrategy) {
        this(partitionNum, capacity);
        this.offerStrategy = offerStrategy;
        this.pollStrategy = pollStrategy;
        this.removeStrategy = removeStrategy;
        if(capacity!=null) {
            this.capacity = capacity * partitionNum;
        }
    }

    public PartiFlow(Integer partitionNum, Integer capacity) {
        partitions = new Partition[partitionNum];
        for (int i = 0; i < partitionNum; i++) {
            partitions[i] = new Partition<T>(capacity);
        }
        if(capacity!=null) {
            this.capacity = capacity * partitionNum;
        }
    }

    public PartiFlow(Integer partitionNum) {
        partitions = new Partition[partitionNum];
        for (int i = 0; i < partitionNum; i++) {
            partitions[i] = new Partition<T>(null);
        }
        if(capacity!=null) {
            this.capacity = capacity * partitionNum;
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

    public T poll(Integer waitTime) throws InterruptedException {
        T element = null;
        int partitionIndex = pollStrategy.selectPartition(partitions);
            if(waitTime==null){//说明无限等待
                int emptyCount = 0;
                while(element==null) {
                    element = partitions[partitionIndex].poll(DEFAULT_WAIT_TIME);
                    if (element == null) {
                        emptyCount++;
                        //所有分区为空就休眠一会，，并且count清零
                        if (emptyCount >= partitions.length) {
                            Thread.sleep(DEFAULT_WAIT_TIME * 5);
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

    public Boolean removeElement() {
            return partitions[removeStrategy.selectPartition(partitions)].removeElement();
    }

    public int getExactElementNums() {
        int sum = 0;
            for (Partition partition : partitions) {
                sum += partition.getElementNums();
            }
            return sum;
    }

    public int getElementNums() {
        int sum = 0;
        for (Partition partition : partitions) {
            sum += partition.getElementNums();
        }
        return sum;
    }

    public void lockGlobally(){
        for(Partition partition : partitions) {
            partition.getTailLock().lock();
            partition.getHeadLock().lock();
        }
    }

    public void unlockGlobally(){
        for(Partition partition : partitions) {
            partition.getHeadLock().unlock();
            partition.getTailLock().unlock();
        }
    }


}
