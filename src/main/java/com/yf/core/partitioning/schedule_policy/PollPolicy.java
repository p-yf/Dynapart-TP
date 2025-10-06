package com.yf.core.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:19
 * @description
 */
public abstract class PollPolicy implements SchedulePolicy{
    public int selectPartition(Partition[] partitions){
        return selectPartition(partitions,null);
    }
    public abstract int selectPartition(Partition[] partitions,Object o);
    public abstract boolean getRoundRobin();
    public abstract void setRoundRobin(boolean roundRobin);
}
