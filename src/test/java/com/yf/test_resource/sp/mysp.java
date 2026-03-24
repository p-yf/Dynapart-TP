package com.yf.test_resource.sp;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;
import com.yf.core.resource_container.scanned_annotation.SPResource;

/**
 * @author yyf
 * @date 2025/9/21 0:57
 * @description 测试用自定义调度策略
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
