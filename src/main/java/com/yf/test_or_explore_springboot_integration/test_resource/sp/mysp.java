package com.yf.test_or_explore_springboot_integration.test_resource.sp;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;
import com.yf.springboot_integration.pool.annotation.SPResource;

/**
 * @author yyf
 * @date 2025/9/21 0:57
 * @description
 */
@SPResource("mysp")
public class mysp extends OfferPolicy {

    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return 0;
    }

    @Override
    public boolean getRoundRobin() {
        return false;
    }

    @Override
    public void setRoundRobin(boolean roundRobin) {

    }
}
