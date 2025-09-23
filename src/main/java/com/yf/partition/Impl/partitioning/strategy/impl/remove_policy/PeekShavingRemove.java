package com.yf.partition.Impl.partitioning.strategy.impl.remove_policy;

import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.partition.Partition;

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
