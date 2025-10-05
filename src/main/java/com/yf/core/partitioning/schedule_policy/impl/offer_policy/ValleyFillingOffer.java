package com.yf.core.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:02
 * @description
 */
public class ValleyFillingOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions,Object object) {
        int minIndex = 0;
        for(int i = 0; i < partitions.length; i++){
            if(partitions[i].getEleNums() < partitions[minIndex].getEleNums()){
                minIndex = i;
            }
        }
        return minIndex;
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
