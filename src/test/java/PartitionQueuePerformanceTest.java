import com.yf.partition.Impl.LinkedBlockingQ;
import com.yf.partition.Impl.LinkedBlockingQS;
import com.yf.partition.Impl.partitioning.PartiFlow;
import com.yf.partition.Impl.partitioning.PartiStill;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.HashOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.ThreadBindingPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.remove_policy.RoundRobinRemove;
import com.yf.partition.Partition;
import com.yf.pool.constant_or_registry.QueueManager;
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
public class PartitionQueuePerformanceTest {
    // 测试参数配置
    private static final int PARTITION_NUM = 64;         // 分区数量
    private static final int CAPACITY = 10000;          // 总容量
    private static final int CORE_THREADS = 256;         // 核心线程数
    private static final int MAX_THREADS = 256;          // 最大线程数
    private static final int TASK_COUNT = 10000000;       // 总任务数
    private static final int CONCURRENT_LEVEL = 256;     // 并发提交线程数
    private static final long AWAIT_TIMEOUT = 60;       // 等待超时时间(秒)


    @Test
    public void testPartitionQueuePerformance() throws InterruptedException {
        int count = 0;
        long total = 0;
        long max = 0;
        long min = Long.MAX_VALUE;
        for(int i = 0;i<20;i++) {
            try {
                /**
                 * 平均每轮：1708.5
                 * 最大：1830
                 * 最小：1537
                 * 失败次数：0
                 */
//                long testTime = testJdkThreadPoolWithLinkedBlockingQueue();
                /**
                 * PartiFlow
                 * 平均每轮：1164.2
                 * 最大：1239
                 * 最小：1093
                 * 失败次数：0
                 *
                 * PartiStill
                 * 平均每轮：1008.7
                 * 最大：1064
                 * 最小：960
                 * 失败次数：0
                 */
                long testTime = testLinkedBlockingQPerformance();

                /**
                 * 平均每轮：1265.4
                 * 最大：1316
                 * 最小：1227
                 * 失败次数：0
                 */
//                long testTime = testLinkedBlockingQProPerformance();
                if(i<=9){
                    continue;
                }
                total += testTime;
                max = Math.max(testTime, max);
                min = Math.min(testTime, min);
            }catch (Exception e){
                count++;
            }
        }
        System.out.println("平均每轮："+(double)total/10);
        System.out.println("最大：" + max);
        System.out.println("最小：" + min);
        System.out.println("失败次数：" + count);
    }

    /**
     * 测试LinkedBlockingQPlus + PartiFlow的性能
     */
//    @Test
    public long testLinkedBlockingQPerformance() throws InterruptedException {
        System.out.println("===== 开始测试 LinkedBlockingQ + PartiFlow 性能 =====");

        // 创建Plus版本的分区队列
        PartiFlow<Runnable> partiFlow = new PartiFlow<>(
                PARTITION_NUM,
                CAPACITY,
                QueueManager.LINKED,
                new HashOffer(),
                new ThreadBindingPoll(),
                new RoundRobinRemove()
        );
        PartiStill<Runnable> partiStill = new PartiStill<>(
                PARTITION_NUM,
                CAPACITY,
                QueueManager.LINKED,
                new HashOffer(),
                new ThreadBindingPoll(),
                new RoundRobinRemove()
        );
        Partition<Runnable> plus = new LinkedBlockingQ<>(CAPACITY);//yes
        Partition<Runnable> pro = new LinkedBlockingQS<>(CAPACITY);
        // 执行性能测试
        return performTest( partiFlow, "LinkedBlockingQ");
    }

    public long testLinkedBlockingQProPerformance() throws InterruptedException {
        System.out.println("===== 开始测试 LinkedBlockingQS + PartiFlow 性能 =====");

        // 创建Plus版本的分区队列
        PartiFlow<Runnable> partiFlow = new PartiFlow<>(
                PARTITION_NUM,
                CAPACITY,
                QueueManager.LINKED_S,
                new HashOffer(),
                new ThreadBindingPoll(),
                new RoundRobinRemove()
        );
        Partition<Runnable> plus = new LinkedBlockingQ<>(CAPACITY);//yes
        Partition<Runnable> pro = new LinkedBlockingQS<>(CAPACITY);
        // 执行性能测试
        return performTest( partiFlow, "LinkedBlockingQS");
    }


    /**
     * 测试3：JDK原生 ThreadPoolExecutor + LinkedBlockingQueue 性能（与自定义队列测试逻辑完全对齐）
     */
//    @Test
    public long testJdkThreadPoolWithLinkedBlockingQueue() throws InterruptedException {
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
                                // 模拟任务：1000次累加（无异常）
                                int sum = 0;
                                for (int k = 0; k < 1000; k++) {
                                    sum += k;
                                }
                            } finally {
                                totalExecutionTime.addAndGet(System.nanoTime() - start);
                                // 修复：先递增实际计数，再唤醒主线程
                                actualTaskCount.incrementAndGet();
                                completeLatch.countDown();
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
            return 0;
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
        return totalTestTime;

//        // 8. 关闭JDK线程池（对应自定义的destroyWorkers）
//        jdkThreadPool.shutdown();
//        if (!jdkThreadPool.awaitTermination(AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
//            jdkThreadPool.shutdownNow();
//        }
    }

    /**
     * 执行具体的性能测试逻辑
     * @param partition 要测试的分区队列
     * @param queueName 队列名称，用于输出结果
     */
    private long performTest(Partition<Runnable> partition, String queueName) throws InterruptedException {
        // 创建线程工厂（修复存活时间单位错误：60秒=60*1000毫秒）
        com.yf.pool.threadfactory.ThreadFactory threadFactory = new com.yf.pool.threadfactory.ThreadFactory(
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
                                // 模拟任务：1000次累加（无异常）
                                int sum = 0;
                                for (int k = 0; k < 1000; k++) {
                                    sum += k;
                                }
                            } finally {
                                totalExecutionTime.addAndGet(System.nanoTime() - start);
                                actualTaskCount.incrementAndGet();
                                completeLatch.countDown();
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
            return 0;
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
//        threadPool.destroyWorkers(CORE_THREADS, MAX_THREADS - CORE_THREADS);
        return totalTestTime;
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
        // 校验任务数量是否匹配
        if (expectedTaskCount != actualTaskCount) {
            System.err.printf("任务数量不匹配：预期%d，实际%d\n", expectedTaskCount, actualTaskCount);
            throw new RuntimeException("任务数量不匹配");
        }
    }
}
