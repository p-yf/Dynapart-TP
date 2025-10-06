package com.yf.core.partitioning.schedule_policy.impl.remove_policy;

import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomRemove extends RemovePolicy {

    @Override
    public int selectPartition(Partition[] partitions,Object o) {
        return (int) (Math.random() * partitions.length);
    }
}
