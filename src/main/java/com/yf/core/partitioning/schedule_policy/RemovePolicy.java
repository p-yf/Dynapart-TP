package com.yf.core.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:24
 * @description
 */
public abstract class RemovePolicy implements SchedulePolicy{
    public int selectPartition(Partition[] partitions){
        return selectPartition(partitions,null);
    }
    public abstract int selectPartition(Partition[] partitions,Object o);
}
