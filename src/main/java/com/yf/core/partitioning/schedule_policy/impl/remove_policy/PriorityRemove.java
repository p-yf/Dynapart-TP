package com.yf.core.partitioning.schedule_policy.impl.remove_policy;

import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;

/**
 * @author yyf
 * @date 2025/10/5 14:06
 * @description
 */
public class PriorityRemove extends RemovePolicy {
    @Override
    public int selectPartition(Partition[] partitions) {
        for(int i = partitions.length-1; i >=0; i--){
            if(partitions[i].getEleNums()>0){
                return i;
            }
        }
        return partitions.length-1;
    }
}
