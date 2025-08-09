package com.yf.pool.taskqueue.Impl.parti_flow;

import com.yf.pool.taskqueue.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.RemoveStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author yyf
 * @date 2025/8/8 17:20
 * @description
 * 分区流：多队列、细粒度、更灵活、高性能
 * 可选任务入队策略：轮询、随机、hash、填谷
 * 可选任务出队策略：轮询、随机、削峰
 * 移除任务策略：轮询、随机、削峰
 */
@Setter
@Getter
public class PartiFlow<T> {
    private Partition<T>[] partitions;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private final Lock coordinationLock = rwLock.readLock();
    private final Lock globalLock = rwLock.writeLock();
    private OfferStrategy offerStrategy = OfferStrategy.ROUND_ROBIN;
    private PollStrategy pollStrategy = PollStrategy.ROUND_ROBIN;
    private RemoveStrategy removeStrategy = RemoveStrategy.ROUND_ROBIN;
    private AtomicInteger offerRound = new AtomicInteger(0);
    private AtomicInteger pollRound = new AtomicInteger(0);
    private AtomicInteger removeRound = new AtomicInteger(0);
    private static final Integer DEFAULT_WAIT_TIME = 100; // 增加默认等待时间到100毫秒

    public PartiFlow(Integer partitionNum, Integer capacity, OfferStrategy offerStrategy, PollStrategy pollStrategy, RemoveStrategy removeStrategy) {
        this(partitionNum, capacity);
        this.offerStrategy = offerStrategy;
        this.pollStrategy = pollStrategy;
        this.removeStrategy = removeStrategy;
    }

    public PartiFlow(Integer partitionNum, Integer capacity) {
        partitions = new Partition[partitionNum];
        for (int i = 0; i < partitionNum; i++) {
            partitions[i] = new Partition<T>(capacity);
        }
    }

    public PartiFlow(Integer partitionNum) {
        partitions = new Partition[partitionNum];
        for (int i = 0; i < partitionNum; i++) {
            partitions[i] = new Partition<T>(null);
        }
    }

    public Boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("元素不能为null");
        }
        coordinationLock.lock();
        try {
            return partitions[offerStrategy.selectPartition(partitions, element)].offer(element);
        } finally {
            coordinationLock.unlock();
        }
    }

    public T poll(Integer waitTime) throws InterruptedException {
        T element = null;
        int partitionIndex = pollStrategy.selectPartition(partitions);
        coordinationLock.lock();
        try{
//            if(waitTime==null){//说明无限等待
//                int emptyCount = 0;
//                while(element==null) {
//                    element = partitions[partitionIndex].poll(DEFAULT_WAIT_TIME);
//                    if (element == null) {
//                        emptyCount++;
//                        // 如果所有分区都连续为空，则增加等待时间
//                        if (emptyCount >= partitions.length) {
//                            Thread.sleep(DEFAULT_WAIT_TIME * 5); // 短暂休眠避免CPU占用过高
//                            emptyCount = 0;
//                        }
//                    } else {
//                        emptyCount = 0;
//                    }
//                    partitionIndex = (partitionIndex+1)%partitions.length;
//                }
//                return element;
//            }else{//说明有等待时间
//                for(int i = 0;i<partitions.length&&element==null;i++) {
//                    element = partitions[partitionIndex].poll(waitTime/partitions.length);
//                    partitionIndex = (partitionIndex+1)%partitions.length;
//                }
//                return element;
//            }
            return partitions[partitionIndex].poll(waitTime);
        }finally {
            coordinationLock.unlock();
        }
    }

    public Boolean removeElement() {
        try{
            coordinationLock.lock();
            return partitions[removeStrategy.selectPartition(partitions)].removeElement();
        }finally {
            coordinationLock.unlock();
        }
    }

    public int getExactElementNums() {
        int sum = 0;
        globalLock.lock();
        try{
            for (Partition partition : partitions) {
                sum += partition.getElementNums();
            }
            return sum;
        }finally {
            globalLock.unlock();
        }
    }

    public int getElementNums() {
        int sum = 0;
        for (Partition partition : partitions) {
            sum += partition.getElementNums();
        }
        return sum;
    }

    /**
     * 清空所有分区的元素
     */
    public void clear() {
        globalLock.lock();
        try {
            for (Partition<T> partition : partitions) {
                while (partition.removeElement()) {
                    // 循环移除直到分区为空
                }
            }
        } finally {
            globalLock.unlock();
        }
    }
}
