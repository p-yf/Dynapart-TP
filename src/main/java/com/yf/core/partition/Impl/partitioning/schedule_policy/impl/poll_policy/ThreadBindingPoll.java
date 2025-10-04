package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.poll_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @date 2025/9/21 0:08
 * @description
 */
public class ThreadBindingPoll extends PollPolicy {
    private volatile boolean roundRobin = false;

    final AtomicLong round = new AtomicLong(0);
    final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    @Override
    public int selectPartition(Partition[] partitions) {
        if(threadLocal.get()==null){
            threadLocal.set((int)round.getAndIncrement()%partitions.length);
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
