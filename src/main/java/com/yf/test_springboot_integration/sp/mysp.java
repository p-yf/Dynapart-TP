package com.yf.test_springboot_integration.sp;

import com.yf.pool.partition.Impl.parti_flow.strategy.OfferPolicy;
import com.yf.pool.partition.Partition;
import com.yf.springboot_integration.pool.annotation.SPResource;

/**
 * @author yyf
 * @date 2025/9/21 0:57
 * @description
 */
@SPResource("mysp")
public class mysp implements OfferPolicy {
    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return 0;
    }
}
