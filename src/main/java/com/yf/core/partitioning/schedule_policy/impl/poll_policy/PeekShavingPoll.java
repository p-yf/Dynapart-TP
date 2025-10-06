package com.yf.core.partitioning.schedule_policy.impl.poll_policy;

import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:07
 * @description
 */
public class PeekShavingPoll extends PollPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions,Object o) {
        int maxIndex = 0;
        for(int i = 0; i < partitions.length; i++){
            if(partitions[i].getEleNums() > partitions[maxIndex].getEleNums()){
                maxIndex = i;
            }
        }
        return maxIndex;
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
