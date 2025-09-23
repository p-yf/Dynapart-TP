package com.yf.partition.Impl.partitioning.strategy.impl.offer_policy;

import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Partition;

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
