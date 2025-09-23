package com.yf.partition.Impl.partitioning.strategy.impl.poll_policy;

import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/21 0:08
 * @description
 */
public class ThreadBindingPoll extends PollPolicy {
    final AtomicInteger round = new AtomicInteger(0);
    final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
    private volatile boolean roundRobin = false;

    @Override
    public int selectPartition(Partition[] partitions) {
        if(threadLocal.get()==null){
            threadLocal.set(round.getAndIncrement()%partitions.length);
        }
        return threadLocal.get();
    }

    @Override
    public boolean getRoundRobin() {
        return roundRobin;
    }

    @Override
    public void setRoundRobin(boolean roundRobin) {
        this.roundRobin = roundRobin;
    }
}
