package com.yf.partition.Impl.partitioning.strategy.impl.poll_policy;

import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/21 0:05
 * @description
 */
public class RoundRobinPoll extends PollPolicy {
    final AtomicInteger round = new AtomicInteger(0);
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions) {
        return Math.abs(round.getAndIncrement()%partitions.length);
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
