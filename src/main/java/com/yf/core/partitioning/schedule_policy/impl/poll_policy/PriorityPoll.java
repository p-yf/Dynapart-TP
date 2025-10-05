package com.yf.core.partitioning.schedule_policy.impl.poll_policy;

import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.PollPolicy;

/**
 * @author yyf
 * @date 2025/10/5 14:03
 * @description
 */
public class PriorityPoll extends PollPolicy {
    private boolean roundRobin = true;
    private volatile RoundRobinPoll roundRobinPoll;
    @Override
    public int selectPartition(Partition[] partitions) {
        for(int i =0;i<partitions.length;i++){
            if(partitions[i].getEleNums()>0){
                return i;
            }
        }
        if(roundRobinPoll==null) {
            synchronized (this) {
                if (roundRobinPoll == null) {
                    roundRobinPoll = new RoundRobinPoll();
                }
            }
        }
        return roundRobinPoll.selectPartition(partitions);
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
