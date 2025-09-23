package com.yf.partition.Impl.partitioning.strategy.impl.poll_policy;

import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomPoll extends PollPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions) {
        return (int) (Math.random() * partitions.length);
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
