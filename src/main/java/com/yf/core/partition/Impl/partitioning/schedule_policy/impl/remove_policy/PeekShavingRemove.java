package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.remove_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:07
 * @description
 */
public class PeekShavingRemove extends RemovePolicy {

    @Override
    public int selectPartition(Partition[] partitions) {
        int maxIndex = 0;
        for (int i = 0; i < partitions.length; i++) {
            if (partitions[i].getEleNums() > partitions[maxIndex].getEleNums()) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}
