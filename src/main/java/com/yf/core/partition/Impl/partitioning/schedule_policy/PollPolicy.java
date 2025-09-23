package com.yf.core.partition.Impl.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:19
 * @description
 */
public abstract class PollPolicy {
    public abstract int selectPartition(Partition[] partitions);
    public abstract boolean getRoundRobin();
    public abstract void setRoundRobin(boolean roundRobin);
}
