package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.remove_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @date 2025/9/21 0:05
 * @description
 */
public class RoundRobinRemove extends RemovePolicy {
    final AtomicLong round = new AtomicLong(0);
    @Override
    public int selectPartition(Partition[] partitions) {
        return (int) round.getAndIncrement()%partitions.length;
    }

}
