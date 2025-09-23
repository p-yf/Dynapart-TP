package com.yf.partition.Impl.partitioning.strategy.impl.poll_policy;

import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:07
 * @description
 */
public class PeekShavingPoll extends PollPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions) {
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
