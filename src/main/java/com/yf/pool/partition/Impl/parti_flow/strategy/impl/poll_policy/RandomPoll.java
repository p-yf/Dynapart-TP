package com.yf.pool.partition.Impl.parti_flow.strategy.impl.poll_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.PollPolicy;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomPoll implements PollPolicy {
    @Override
    public int selectPartition(Partition[] partitions) {
        return (int) (Math.random() * partitions.length);
    }
}
