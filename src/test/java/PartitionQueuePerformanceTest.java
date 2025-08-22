import com.yf.pool.constant.OfQueue;
import com.yf.pool.partition.Impl.LinkedBlockingQPlus;
import com.yf.pool.partition.Impl.parti_flow.PartiFlow;
import com.yf.pool.partition.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.RemoveStrategy;
import com.yf.pool.partition.Partition;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.threadpool.ThreadPool;
import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分区队列性能比较测试
 * 测试LinkedBlockingQPro和LinkedBlockingQPlus在PartiFlow包装下的性能差异
 */
public class PartitionQueuePerformanceTest {//     分区化            |        无分区化
    //   |    分区  |   线程数  |    pro   |     plus    |    mini    |     plus(无分区化)    |    pro(无分区化)  |  jdk+linked
    //   |    4    |    16    |    1857  |     1968    |            |                     |                  |
    //   |    8    |    16    |    1383  |     1647    |            |                     |                  |
    //   |    16   |    16    |    1255  |     1534    |            |                     |                  |
    //   |         |          |          |             |            |                     |                  |
    //   |    16   |     32   |    1160  |     1670    |     1676   |     1848   2674     |    1864    2523  |  2142   2679
    //   |    16   |     64   |    1410  |     1730    |     2004   |     1987   3877     |    1997    2913  |  2207   4340
    //   |    16   |     128  |    1338  |     2269    |     2873   |     2474   4075     |    3047    3971  |  2729   4453
    // 测试参数配置
    private static final int PARTITION_NUM = 16;         // 分区数量
    private static final int CAPACITY = 10000;          // 总容量
    private static final int CORE_THREADS = 128;         // 核心线程数
    private static final int MAX_THREADS = 128;          // 最大线程数
    private static final int TASK_COUNT = 10000000;       // 总任务数
    private static final int CONCURRENT_LEVEL = 64;     // 并发提交线程数
    private static final long AWAIT_TIMEOUT = 60;       // 等待超时时间(秒)


    /**
     * 测试LinkedBlockingQPro + PartiFlow的性能
     */
    @Test
    public void testLinkedBlockingQProPerformance() throws InterruptedException {
        System.out.println("===== 开始测试 LinkedBlockingQPro + PartiFlow 性能 =====");

        // 创建Pro版本的分区队列
        PartiFlow<Runnable> proPartition = new PartiFlow<>(
                PARTITION_NUM,
                CAPACITY,
                OfQueue.LINKED_MINI,
                OfferStrategy.HASH,
                PollStrategy.THREAD_BINDING,
                RemoveStrategy.ROUND_ROBIN
        );

        // 执行性能测试
        performTest(proPartition, "LinkedBlockingQPro");
    }

    /**
     * 测试LinkedBlockingQPlus + PartiFlow的性能
     */
    @Test
    public void testLinkedBlockingQPlusPerformance() throws InterruptedException {
        System.out.println("===== 开始测试 LinkedBlockingQPlus + PartiFlow 性能 =====");

        // 创建Plus版本的分区队列
        PartiFlow<Runnable> plusPartition = new PartiFlow<>(
                PARTITION_NUM,
                CAPACITY,
                OfQueue.LINKED_PLUS,
                OfferStrategy.HASH,
                PollStrategy.THREAD_BINDING,
                RemoveStrategy.ROUND_ROBIN
        );
        Partition<Runnable> partition = new LinkedBlockingQPlus<>(CAPACITY);
        // 执行性能测试
        performTest( partition, "LinkedBlockingQPlus");
    }

    /**
     * 测试3：JDK原生 ThreadPoolExecutor + LinkedBlockingQueue 性能（与自定义队列测试逻辑完全对齐）
     */
    @Test
    public void testJdkThreadPoolWithLinkedBlockingQueue() throws InterruptedException {
        System.out.println("===== 开始测试 JDK ThreadPoolExecutor + LinkedBlockingQueue 性能 =====");

        // 1. 初始化JDK队列（容量与自定义队列总容量一致：CAPACITY=10000）
        BlockingQueue<Runnable> jdkLinkedQueue = new LinkedBlockingQueue<>(CAPACITY);

        // 2. 初始化JDK线程池（参数严格对齐自定义线程池）
        ThreadPoolExecutor jdkThreadPool = new ThreadPoolExecutor(
                CORE_THREADS,          // 核心线程数：32（与自定义一致）
                MAX_THREADS,           // 最大线程数：32（与自定义一致，固定线程池）
                60 * 1000,             // 空闲存活时间：60秒（与自定义线程工厂aliveTime一致）
                TimeUnit.MILLISECONDS, // 时间单位：毫秒（与自定义线程工厂单位一致）
                jdkLinkedQueue,
                new java.util.concurrent.ThreadFactory() { // 线程配置对齐自定义
                    private final AtomicInteger threadNum = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "jdk-worker-" + threadNum.getAndIncrement());
                        thread.setDaemon(false); // 非守护线程（与自定义一致）
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略对齐自定义CallerRunsStrategy
        );

        // 3. 测试任务逻辑（与自定义测试完全一致）
        AtomicLong totalExecutionTime = new AtomicLong(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(TASK_COUNT);
        AtomicInteger actualTaskCount = new AtomicInteger(0);

        // 4. 并发提交线程（数量、任务分配逻辑与自定义一致）
        List<Thread> submitThreads = new ArrayList<>(CONCURRENT_LEVEL);
        for (int i = 0; i < CONCURRENT_LEVEL; i++) {
            final int threadIndex = i;
            Thread submitThread = new Thread(() -> {
                try {
                    startLatch.await();
                    // 任务分配：处理余数，确保总任务数=1000万（与自定义一致）
                    int baseTasks = TASK_COUNT / CONCURRENT_LEVEL;
                    int remainingTasks = TASK_COUNT % CONCURRENT_LEVEL;
                    int tasksPerThread = baseTasks + (threadIndex < remainingTasks ? 1 : 0);

                    for (int j = 0; j < tasksPerThread; j++) {
                        jdkThreadPool.execute(() -> {
                            long start = System.nanoTime();
                            try {
                                // 模拟任务：1000次累加（与自定义完全一致）
                                int sum = 0;
                                for (int k = 0; k < 1000; k++) {
                                    sum += k;
                                }
                            } finally {
                                totalExecutionTime.addAndGet(System.nanoTime() - start);
                                completeLatch.countDown();
                                actualTaskCount.incrementAndGet();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "submit-thread-" + i);
            submitThreads.add(submitThread);
        }

        // 5. 启动测试与计时（与自定义一致）
        long testStartTime = System.currentTimeMillis();
        submitThreads.forEach(Thread::start);
        startLatch.countDown();

        // 6. 等待完成+超时保护（与自定义一致）
        boolean isCompleted = completeLatch.await(AWAIT_TIMEOUT, TimeUnit.SECONDS);
        if (!isCompleted) {
            System.err.println("JDK测试超时！未完成的任务数：" + completeLatch.getCount());
            System.err.println("JDK测试实际提交的任务数：" + actualTaskCount.get());
            return;
        }

        // 7. 结果计算与输出（调用你现有的printTestResults方法，格式完全一致）
        long totalTestTime = System.currentTimeMillis() - testStartTime;
        printTestResults(
                "JDK-ThreadPool+LinkedBlockingQueue",
                totalTestTime,
                totalExecutionTime.get(),
                TASK_COUNT,
                actualTaskCount.get()
        );

        // 8. 关闭JDK线程池（对应自定义的destroyWorkers）
        jdkThreadPool.shutdown();
        if (!jdkThreadPool.awaitTermination(AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
            jdkThreadPool.shutdownNow();
        }
    }

    /**
     * 执行具体的性能测试逻辑
     * @param partition 要测试的分区队列
     * @param queueName 队列名称，用于输出结果
     */
    private void performTest(Partition<Runnable> partition, String queueName) throws InterruptedException {
        // 创建线程工厂（修复存活时间单位错误：60秒=60*1000毫秒）
        ThreadFactory threadFactory = new ThreadFactory(
                queueName + "-worker",
                false,  // 非守护线程
                false,  // 核心线程不销毁
                60 * 1000      // 空闲存活时间(毫秒)
        );

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(
                CORE_THREADS,
                MAX_THREADS,
                queueName + "-pool",
                threadFactory,
                partition,
                new CallerRunsStrategy()
        );

        // 准备测试任务
        AtomicLong totalExecutionTime = new AtomicLong(0);
        CountDownLatch startLatch = new CountDownLatch(1);  // 同步开始信号
        CountDownLatch completeLatch = new CountDownLatch(TASK_COUNT);  // 任务完成信号
        AtomicInteger actualTaskCount = new AtomicInteger(0);  // 实际提交的任务数（用于校验）

        // 创建并发提交线程
        List<Thread> submitThreads = new ArrayList<>(CONCURRENT_LEVEL);
        for (int i = 0; i < CONCURRENT_LEVEL; i++) {
            final int threadIndex = i;
            Thread submitThread = new Thread(() -> {
                try {
                    startLatch.await();  // 等待开始信号

                    // 计算当前线程需要提交的任务数（修复整数除法导致的任务缺失）
                    int baseTasks = TASK_COUNT / CONCURRENT_LEVEL;
                    int remainingTasks = TASK_COUNT % CONCURRENT_LEVEL;
                    int tasksPerThread = baseTasks + (threadIndex < remainingTasks ? 1 : 0);

                    for (int j = 0; j < tasksPerThread; j++) {
                        threadPool.execute(() -> {
                            long start = System.nanoTime();
                            try {
                                // 模拟任务执行（微秒级计算）
                                int sum = 0;
                                for (int k = 0; k < 1000; k++) {
                                    sum += k;
                                }
                            } finally {
                                totalExecutionTime.addAndGet(System.nanoTime() - start);
                                completeLatch.countDown();
                                actualTaskCount.incrementAndGet();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "submit-thread-" + i);
            submitThreads.add(submitThread);
        }

        // 记录开始时间
        long testStartTime = System.currentTimeMillis();

        // 启动所有提交线程
        submitThreads.forEach(Thread::start);
        startLatch.countDown();  // 发出开始信号

        // 等待所有任务完成（增加超时机制，避免无限等待）
        boolean isCompleted = completeLatch.await(AWAIT_TIMEOUT, TimeUnit.SECONDS);
        if (!isCompleted) {
            System.err.println("测试超时！未完成的任务数：" + completeLatch.getCount());
            System.err.println("实际提交的任务数：" + actualTaskCount.get());
            return;
        }

        // 记录结束时间
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        // 输出测试结果
        printTestResults(
                queueName,
                totalTestTime,
                totalExecutionTime.get(),
                TASK_COUNT,
                actualTaskCount.get()
        );

        // 关闭线程池（先确认任务已完成）
        threadPool.destroyWorkers(CORE_THREADS, MAX_THREADS - CORE_THREADS);
    }

    /**
     * 打印测试结果
     */
    private void printTestResults(
            String queueName,
            long totalTestTime,
            long totalExecutionTime,
            int expectedTaskCount,
            int actualTaskCount) {

        // 校验任务数量是否匹配
        if (expectedTaskCount != actualTaskCount) {
            System.err.printf("任务数量不匹配：预期%d，实际%d\n", expectedTaskCount, actualTaskCount);
        }

        // 计算吞吐量（任务/秒）
        double throughput = (double) expectedTaskCount / (totalTestTime / 1000.0);

        // 计算平均延迟（毫秒）
        double avgLatency = (double) totalExecutionTime / expectedTaskCount / 1_000_000;

        System.out.printf("[%s] 测试结果:\n", queueName);
        System.out.printf("  总任务数: %d\n", expectedTaskCount);
        System.out.printf("  总耗时: %d ms\n", totalTestTime);
        System.out.printf("  吞吐量: %.2f 任务/秒\n", throughput);
        System.out.printf("  平均任务执行延迟: %.4f ms\n", avgLatency);
        System.out.println("========================================\n");
    }
}
