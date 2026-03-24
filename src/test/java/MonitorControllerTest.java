
import com.yf.common.entity.PoolInfo;
import com.yf.common.entity.QueueInfo;
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.rejectstrategy.impl.DiscardStrategy;
import com.yf.core.resource_container.resource_manager.PartiResourceManager;
import com.yf.core.resource_container.resource_manager.RSResourceManager;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitorControllerTest {

    private static final String TEST_POOL_NAME = "testPool";
    private static final String TEST_POOL_NAME_2 = "testPool2";
    private static ThreadPool testThreadPool;
    private static ThreadPool testThreadPool2;

    @BeforeAll
    public static void setupPool() {
        // Register a test queue
        PartiResourceManager.register("testLinked", LinkedBlockingQ.class);
        RSResourceManager.register("discard", DiscardStrategy.class);

        // Create test thread pools
        WorkerFactory factory = new WorkerFactory("test-thread", true, false, 5000);
        testThreadPool = new ThreadPool(5, 10, TEST_POOL_NAME, factory, new LinkedBlockingQ<>(100), new DiscardStrategy());

        WorkerFactory factory2 = new WorkerFactory("test-thread2", true, false, 5000);
        testThreadPool2 = new ThreadPool(2, 4, TEST_POOL_NAME_2, factory2, new LinkedBlockingQ<>(50), new DiscardStrategy());
    }

    @AfterAll
    public static void cleanup() {
        UnifiedTPRegulator.unregister(TEST_POOL_NAME);
        UnifiedTPRegulator.unregister(TEST_POOL_NAME_2);
    }

    @Test
    @Order(1)
    public void testGetAllPools() {
        java.util.List<PoolInfo> pools = UnifiedTPRegulator.getAllThreadPoolInfo();
        assertNotNull(pools);
        assertTrue(pools.size() > 0, "Should have at least one thread pool");

        boolean found = pools.stream().anyMatch(p -> TEST_POOL_NAME.equals(p.getPoolName()));
        assertTrue(found, "Test pool should be in the list");
    }

    @Test
    @Order(2)
    public void testGetAllPoolNames() {
        java.util.List<String> names = UnifiedTPRegulator.getAllThreadPoolNames();
        assertNotNull(names);
        assertTrue(names.contains(TEST_POOL_NAME), "Pool names should contain test pool");
    }

    @Test
    @Order(3)
    public void testGetThreadPoolInfo() {
        PoolInfo info = UnifiedTPRegulator.getThreadPoolInfo(TEST_POOL_NAME);
        assertNotNull(info);
        assertEquals(TEST_POOL_NAME, info.getPoolName());
        assertEquals(5, info.getCoreNums());
        assertEquals(10, info.getMaxNums());
    }

    @Test
    @Order(4)
    public void testGetTaskNums() {
        // Submit some tasks first
        for (int i = 0; i < 5; i++) {
            testThreadPool.execute(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            });
        }

        int taskNums = UnifiedTPRegulator.getTaskNums(TEST_POOL_NAME);
        assertTrue(taskNums >= 0, "Task count should be non-negative");
    }

    @Test
    @Order(5)
    public void testGetPartitionTaskNums() {
        Map<Integer, Integer> partitionNums = UnifiedTPRegulator.getPartitionTaskNums(TEST_POOL_NAME);
        assertNotNull(partitionNums);
        assertFalse(partitionNums.isEmpty());
    }

    @Test
    @Order(6)
    public void testGetQueueInfo() {
        // Use testThreadPool2 to avoid interference from other tests
        QueueInfo queueInfo = UnifiedTPRegulator.getQueueInfo(TEST_POOL_NAME_2);
        assertNotNull(queueInfo);
        assertEquals(50, queueInfo.getCapacity());
    }

    @Test
    @Order(7)
    public void testGetAllQueueName() {
        java.util.List<String> queueNames = UnifiedTPRegulator.getAllQueueName();
        assertNotNull(queueNames);
        assertTrue(queueNames.contains("linked") || queueNames.contains("testLinked"),
                "Should contain at least one registered queue");
    }

    @Test
    @Order(8)
    public void testGetAllRejectStrategyName() {
        java.util.List<String> strategyNames = UnifiedTPRegulator.getAllRejectStrategyName();
        assertNotNull(strategyNames);
        assertTrue(strategyNames.contains("discard"), "Should contain discard strategy");
    }

    @Test
    @Order(9)
    public void testChangeWorkerParams() {
        // Save original values
        int originalCore = testThreadPool.getCoreNums();
        int originalMax = testThreadPool.getMaxNums();

        try {
            // Test changing core and max nums
            Boolean result = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME, 8, 15, null, null, null);
            assertTrue(result, "Change worker params should succeed");

            assertEquals(8, testThreadPool.getCoreNums());
            assertEquals(15, testThreadPool.getMaxNums());

            // Test validation - core should not exceed max
            Boolean failResult = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME, 20, 10, null, null, null);
            assertFalse(failResult, "Change should fail when core > max");
        } finally {
            // Restore original values
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME, originalCore, originalMax, null, null, null);
        }
    }

    @Test
    @Order(10)
    public void testChangeWorkerParamsWithNullCoreNums() {
        // Use testThreadPool2 to avoid interference
        int originalCore = testThreadPool2.getCoreNums();
        int originalMax = testThreadPool2.getMaxNums();

        try {
            Boolean result = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, null, 8, null, null, null);
            assertTrue(result, "Change should succeed with null core");
            assertEquals(8, testThreadPool2.getMaxNums());
        } finally {
            // Restore using non-null values to avoid NPE in implementation
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, originalCore, originalMax, null, null, null);
        }
    }

    @Test
    @Order(11)
    public void testChangeWorkerParamsWithNullMaxNums() {
        // Use testThreadPool2 to avoid interference
        int originalCore = testThreadPool2.getCoreNums();
        int originalMax = testThreadPool2.getMaxNums();

        try {
            Boolean result = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, 1, null, null, null, null);
            assertTrue(result, "Change should succeed with null max");
        } finally {
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, originalCore, originalMax, null, null, null);
        }
    }

    @Test
    @Order(12)
    public void testChangeWorkerAliveTime() {
        // Use testThreadPool2 to avoid interference
        int originalCore = testThreadPool2.getCoreNums();
        int originalMax = testThreadPool2.getMaxNums();
        int originalAliveTime = testThreadPool2.getWorkerFactory().getAliveTime();

        try {
            Boolean result = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, null, null, null, 10000, null);
            assertTrue(result, "Change alive time should succeed");
            assertEquals(10000, testThreadPool2.getWorkerFactory().getAliveTime());
        } finally {
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, originalCore, originalMax, null, originalAliveTime, null);
        }
    }

    @Test
    @Order(13)
    public void testChangeWorkerCoreDestroy() {
        // Use testThreadPool2 to avoid interference
        int originalCore = testThreadPool2.getCoreNums();
        int originalMax = testThreadPool2.getMaxNums();
        boolean originalCoreDestroy = testThreadPool2.getWorkerFactory().isCoreDestroy();

        try {
            Boolean result = UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, null, null, true, null, null);
            assertTrue(result, "Change core destroy should succeed");
            assertTrue(testThreadPool2.getWorkerFactory().isCoreDestroy());
        } finally {
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, originalCore, originalMax, originalCoreDestroy, null, null);
        }
    }

    @Test
    @Order(14)
    public void testChangeQueueCapacity() {
        // Use testThreadPool2 to avoid interference
        int originalCore = testThreadPool2.getCoreNums();
        int originalMax = testThreadPool2.getMaxNums();

        try {
            UnifiedTPRegulator.changeQueueCapacity(TEST_POOL_NAME_2, 200);
            // The capacity change should be reflected
            assertEquals(200, testThreadPool2.getPartition().getCapacity());
        } finally {
            UnifiedTPRegulator.changeWorkerParams(TEST_POOL_NAME_2, originalCore, originalMax, null, null, null);
        }
    }

    @Test
    @Order(15)
    public void testGetThreadsInfo() {
        Map<String, Map<Thread.State, Integer>> threadsInfo = UnifiedTPRegulator.getThreadsInfo(TEST_POOL_NAME);
        assertNotNull(threadsInfo);
        assertTrue(threadsInfo.containsKey("core") || threadsInfo.containsKey("extra"));

        Map<String, Map<Thread.State, Integer>> allThreadsInfo = UnifiedTPRegulator.getThreadsInfo(TEST_POOL_NAME);
        assertNotNull(allThreadsInfo.get("core"));
        assertNotNull(allThreadsInfo.get("extra"));
    }

    @Test
    @Order(16)
    public void testThreadPoolWithNoTasks() {
        String emptyPoolName = "emptyPoolTest";
        WorkerFactory emptyFactory = new WorkerFactory("empty-thread", true, false, 5000);
        ThreadPool emptyPool = new ThreadPool(2, 4, emptyPoolName, emptyFactory, new LinkedBlockingQ<>(50), new DiscardStrategy());
        UnifiedTPRegulator.register(emptyPoolName, emptyPool);

        try {
            int taskNums = UnifiedTPRegulator.getTaskNums(emptyPoolName);
            assertEquals(0, taskNums, "Empty pool should have 0 tasks");

            QueueInfo queueInfo = UnifiedTPRegulator.getQueueInfo(emptyPoolName);
            assertNotNull(queueInfo);
            assertEquals(50, queueInfo.getCapacity());
        } finally {
            UnifiedTPRegulator.unregister(emptyPoolName);
        }
    }

    @Test
    @Order(17)
    public void testNonExistentPoolReturnsNull() {
        // Test getThreadPoolInfo with non-existent pool
        PoolInfo info = UnifiedTPRegulator.getThreadPoolInfo("nonExistentPool");
        assertNull(info, "Non-existent pool should return null for pool info");

        // Test getTaskNums with non-existent pool - it returns 0 according to the implementation
        int taskNums = UnifiedTPRegulator.getTaskNums("nonExistentPool");
        assertEquals(0, taskNums, "Non-existent pool should return 0 for task nums");
    }

    @Test
    @Order(18)
    public void testRejectStrategyChange() {
        // Test changing reject strategy
        DiscardStrategy newStrategy = new DiscardStrategy();
        Boolean result = testThreadPool.changeRejectStrategy(newStrategy, "discard");
        assertTrue(result, "Reject strategy change should succeed");
    }
}
