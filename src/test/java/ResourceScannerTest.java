package com.yf.test_resource;

import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.resource_container.scanned_annotation.ResourceScan;
import com.yf.core.resource_container.ResourceScanner;
import com.yf.core.resource_container.resource_manager.PartiResourceManager;
import com.yf.core.resource_container.resource_manager.RSResourceManager;
import com.yf.core.resource_container.resource_manager.SPResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源扫描测试
 */
@ResourceScan
public class ResourceScannerTest {

    @Test
    public void testResourceScan() {
        // 手动调用scan扫描com.yf.test_resource包下的资源
        ResourceScanner.scan(ResourceScannerTest.class);

        // 验证队列资源注册 - myq.java
        Class<? extends Partition> queueClass = PartiResourceManager.getResource("myq");
        assertNotNull(queueClass, "队列资源myq应该被注册");
        System.out.println("✓ 队列资源注册: " + queueClass.getName());

        // 验证拒绝策略资源注册 - mys.java
        Class<? extends RejectStrategy> rsClass = RSResourceManager.getResource("mys");
        assertNotNull(rsClass, "拒绝策略资源mys应该被注册");
        System.out.println("✓ 拒绝策略资源注册: " + rsClass.getName());

        // 验证调度策略资源注册 - mysp.java
        Class<? extends OfferPolicy> spClass = SPResourceManager.getOfferResource("mysp");
        assertNotNull(spClass, "调度策略资源mysp应该被注册");
        System.out.println("✓ 调度策略资源注册: " + spClass.getName());

        System.out.println("✓ 资源扫描测试全部通过");
    }
}
