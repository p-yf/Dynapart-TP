package com.yf.pool.partition.Impl.parti_flow.strategy.impl.offer_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.OfferPolicy;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:02
 * @description
 */
public class ValleyFillingOffer implements OfferPolicy {
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
}
