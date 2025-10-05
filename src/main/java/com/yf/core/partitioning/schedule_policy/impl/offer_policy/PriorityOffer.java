package com.yf.core.partitioning.schedule_policy.impl.offer_policy;

import com.yf.common.task.Priority;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;

/**
 * @author yyf
 * @date 2025/10/5 13:50
 * @description
 */
public class PriorityOffer extends OfferPolicy {
    private boolean roundRobin = true;
    private volatile RoundRobinOffer roundRobinOffer;
    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        if(object instanceof Priority){
            int priority = ((Priority) object).getPriority();
            for(;priority<partitions.length;priority++){
                if(partitions[priority].getEleNums()<partitions[priority].getCapacity()){
                    return priority;
                }
            }
        }

        //如果并非优先级实例则降级为轮询（double check的饿汉型单例模式）
        if(roundRobinOffer==null) {
            synchronized (this) {
                if (roundRobinOffer == null) {
                    roundRobinOffer = new RoundRobinOffer();
                }
            }
        }
        return roundRobinOffer.selectPartition(partitions,object);
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
