package com.yf.pool.partition.Impl.parti_flow.strategy.impl.poll_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.PollPolicy;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:07
 * @description
 */
public class PeekShavingPoll implements PollPolicy {
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
}
