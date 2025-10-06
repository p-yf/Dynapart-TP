package com.yf.core.partitioning.schedule_policy.impl.poll_policy;

import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomPoll extends PollPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions,Object o) {
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
