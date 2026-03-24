import com.yf.core.partition.Partition;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.resource_container.resource_manager.PartiResourceManager;
import com.yf.core.resource_container.resource_manager.RSResourceManager;
import com.yf.core.resource_container.resource_manager.SPResourceManager;
import com.yf.core.resource_container.scanned_annotation.GCTResource;
import com.yf.core.resource_container.scanned_annotation.PartiResource;
import com.yf.core.resource_container.scanned_annotation.RSResource;
import com.yf.core.resource_container.scanned_annotation.SPResource;
import com.yf.springboot_integration.monitor.controller.MonitorController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class HotDeployTest {

    private MonitorController monitorController;
    private ApplicationContext mockContext;

    @BeforeEach
    public void setup() {
        mockContext = Mockito.mock(ApplicationContext.class);
        monitorController = new MonitorController(mockContext);
    }

    @Test
    public void testHotDeployQueue() {
        String className = "com.yf.TestQueue";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.core.partition.Impl.LinkedBlockingQ;\n" +
            "import com.yf.core.resource_container.scanned_annotation.PartiResource;\n" +
            "@PartiResource(\"testQ\")\n" +
            "public class TestQueue extends LinkedBlockingQ<Runnable> {\n" +
            "    public TestQueue(Integer capacity) {\n" +
            "        super(capacity);\n" +
            "    }\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "Queue hot deploy failed");

        Class<? extends Partition> registered = PartiResourceManager.getResource("testQ");
        Assertions.assertNotNull(registered, "Queue was not registered in PartiResourceManager");
        Assertions.assertEquals(className, registered.getName());
    }

    @Test
    public void testHotDeployRejectStrategy() {
        String className = "com.yf.TestRejectStrategy";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.core.rejectstrategy.RejectStrategy;\n" +
            "import com.yf.core.threadpool.ThreadPool;\n" +
            "import com.yf.core.resource_container.scanned_annotation.RSResource;\n" +
            "@RSResource(\"testRS\")\n" +
            "public class TestRejectStrategy extends RejectStrategy {\n" +
            "    @Override\n" +
            "    public void reject(ThreadPool threadPool, Runnable task) {}\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "RejectStrategy hot deploy failed");

        Class<? extends RejectStrategy> registered = RSResourceManager.getResource("testRS");
        Assertions.assertNotNull(registered, "RejectStrategy was not registered in RSResourceManager");
        Assertions.assertEquals(className, registered.getName());
    }

    @Test
    public void testHotDeploySchedulePolicy() {
        String className = "com.yf.TestOfferPolicy";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.core.partitioning.schedule_policy.OfferPolicy;\n" +
            "import com.yf.core.partition.Partition;\n" +
            "import com.yf.core.resource_container.scanned_annotation.SPResource;\n" +
            "@SPResource(\"testSP\")\n" +
            "public class TestOfferPolicy extends OfferPolicy {\n" +
            "    @Override\n" +
            "    public int selectPartition(Partition[] partitions, Object o) { return 0; }\n" +
            "    @Override\n" +
            "    public boolean getRoundRobin() { return false; }\n" +
            "    @Override\n" +
            "    public void setRoundRobin(boolean roundRobin) {}\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "SchedulePolicy hot deploy failed");

        Class<? extends OfferPolicy> registered = SPResourceManager.getOfferResource("testSP");
        Assertions.assertNotNull(registered, "SchedulePolicy was not registered in SPResourceManager");
        Assertions.assertEquals(className, registered.getName());
    }

    @Test
    public void testHotDeployGCTaskBindingPartition() {
        // Register a partition resource to bind to
        PartiResourceManager.register("myq_for_gc", com.yf.core.partition.Impl.LinkedBlockingQ.class);

        String className = "com.yf.TestGCTaskPartition";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.common.task.GCTask;\n" +
            "import com.yf.core.resource_container.scanned_annotation.GCTResource;\n" +
            "@GCTResource(bindingPartiResource = \"myq_for_gc\")\n" +
            "public class TestGCTaskPartition extends GCTask {\n" +
            "    @Override\n" +
            "    public void run() {}\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "GCTask binding partition hot deploy failed");
    }

    @Test
    public void testHotDeployGCTaskBindingSchedulePolicyWithAutoColon() {
        // Register a schedule policy to bind to
        SPResourceManager.register("mysp_for_gc", com.yf.core.partitioning.schedule_policy.impl.poll_policy.ThreadBindingPoll.class);

        String className = "com.yf.TestGCTaskSPAutoColon";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.common.task.GCTask;\n" +
            "import com.yf.core.resource_container.scanned_annotation.GCTResource;\n" +
            "@GCTResource(bindingSPResource = \"mysp_for_gc\", spType = \"poll\")\n" + // Note: "poll" instead of "poll:"
            "public class TestGCTaskSPAutoColon extends GCTask {\n" +
            "    @Override\n" +
            "    public void run() {}\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "GCTask with spType auto-colon hot deploy failed");
    }

    @Test
    public void testHotDeployGCTaskBindingSchedulePolicyOffer() {
        // Register an offer policy to bind to
        SPResourceManager.register("myoffer_for_gc", com.yf.core.partitioning.schedule_policy.impl.offer_policy.RoundRobinOffer.class);

        String className = "com.yf.TestGCTaskOffer";
        String javaCode =
            "package com.yf;\n" +
            "import com.yf.common.task.GCTask;\n" +
            "import com.yf.core.resource_container.scanned_annotation.GCTResource;\n" +
            "@GCTResource(bindingSPResource = \"myoffer_for_gc\", spType = \"offer:\")\n" +
            "public class TestGCTaskOffer extends GCTask {\n" +
            "    @Override\n" +
            "    public void run() {}\n" +
            "}";

        Map<String, Object> result = monitorController.hotDeploy(className, javaCode);
        Assertions.assertTrue((Boolean) result.get("success"), "GCTask binding offer policy hot deploy failed");
    }
}
