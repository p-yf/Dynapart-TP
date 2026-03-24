
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Impl.LinkedBlockingQS;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.impl.PartiStill;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.*;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.*;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.*;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.rejectstrategy.impl.AbortStrategy;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.rejectstrategy.impl.DiscardOldestStrategy;
import com.yf.core.rejectstrategy.impl.DiscardStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 并发压力测试类 - 测试所有资源配置组合
 * 测试所有队列类型、拒绝策略、调度策略组合
 */
@Slf4j
public class ConcurrencyStressTest {

    // 测试配置
    private static final int THREAD_COUNT = 20;           // 并发线程数
    private static final int TASK_COUNT_PER_THREAD = 1000; // 每个线程任务数
    private static final int QUEUE_CAPACITY = 1000;        // 队列容量
    private static final int PARTITION_NUM = 4;            // 分区数量
    private static final long TEST_DURATION_MS = 10000;    // 动态切换测试持续时间

    // 结果收集
    private static final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private static final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private static final AtomicLong totalErrors = new AtomicLong(0);
    private static final AtomicLong configSwitchCount = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        log.info("========== 并发压力测试开始 ==========");

        // 1. 测试非分区队列 + 所有拒绝策略
        testNonPartitionedQueues();

        // 2. 测试分区队列(PartiFlow) + 所有调度策略组合
        testPartiFlowWithAllSchedulePolicies();

        // 3. 测试分区队列(PartiStill) + 所有调度策略组合
        testPartiStillWithAllSchedulePolicies();

        // 4. 测试配置动态切换的并发安全性
        testConfigDynamicSwitch();

        // 5. 压力测试 - 高并发极端情况
        stressTestHighConcurrency();

        log.info("========== 并发压力测试完成 ==========");
        log.info("总提交任务: {}, 总完成任务: {}, 总错误: {}, 配置切换次数: {}",
                totalTasksSubmitted.get(), totalTasksCompleted.get(), totalErrors.get(), configSwitchCount.get());
    }

    /**
     * 测试非分区队列 + 所有拒绝策略组合
     */
    private static void testNonPartitionedQueues() throws Exception {
        log.info("\n========== 测试非分区队列 + 拒绝策略 ==========");

        // 队列类型
        Map<String, Class<? extends Partition>> queueTypes = new HashMap<>();
        queueTypes.put("LinkedBlockingQ", LinkedBlockingQ.class);
        queueTypes.put("LinkedBlockingQS", LinkedBlockingQS.class);

        // 拒绝策略
        Map<String, RejectStrategy> rejectStrategies = new HashMap<>();
        rejectStrategies.put("CallerRuns", new CallerRunsStrategy());
        rejectStrategies.put("DiscardOldest", new DiscardOldestStrategy());
        rejectStrategies.put("Discard", new DiscardStrategy());
        rejectStrategies.put("Abort", new AbortStrategy());

        for (Map.Entry<String, Class<? extends Partition>> queueEntry : queueTypes.entrySet()) {
            for (Map.Entry<String, RejectStrategy> rsEntry : rejectStrategies.entrySet()) {
                String testName = "NonParti[" + queueEntry.getKey() + "]+RS[" + rsEntry.getKey() + "]";
                testNonPartitionedQueue(queueEntry.getValue(), queueEntry.getKey(),
                        rsEntry.getValue(), rsEntry.getKey(), testName);
            }
        }
    }

    private static void testNonPartitionedQueue(Class<? extends Partition> queueClass,
                                                 String queueName,
                                                 RejectStrategy rejectStrategy,
                                                 String rsName,
                                                 String testName) throws Exception {
        String poolName = "Test_" + testName.replaceAll("[\\[\\]]", "_");
        log.info("测试: {}", testName);

        try {
            Partition<Runnable> queue = createQueue(queueClass, QUEUE_CAPACITY);
            WorkerFactory factory = new WorkerFactory("test-worker", true, false, 5000);
            RejectStrategy rs = createRejectStrategy(rejectStrategy);

            ThreadPool pool = new ThreadPool(5, 10, poolName, factory, queue, rs);
            UnifiedTPRegulator.unregister(poolName);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // 提交任务
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < TASK_COUNT_PER_THREAD; j++) {
                            try {
                                int taskId = threadId * TASK_COUNT_PER_THREAD + j;
                                pool.execute(() -> {
                                    try {
                                        Thread.sleep(1);
                                        totalTasksCompleted.incrementAndGet();
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                });
                                successCount.incrementAndGet();
                                totalTasksSubmitted.incrementAndGet();
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                                totalErrors.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

            // 等待任务处理
            Thread.sleep(2000);

            int remaining = pool.getTaskNums();
            log.info("  {} - 成功提交: {}, 错误: {}, 剩余队列: {}, 完成: {}",
                    testName, successCount.get(), errorCount.get(), remaining, totalTasksCompleted.get());

            // 验证
            if (errorCount.get() > 0 && !(rs instanceof CallerRunsStrategy)) {
                log.error("  !!! {} - 出现意外错误!", testName);
            }

            pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
            UnifiedTPRegulator.unregister(poolName);

        } catch (Exception e) {
            log.error("  !!! {} - 测试异常: {}", testName, e.getMessage(), e);
            totalErrors.incrementAndGet();
        }
    }

    /**
     * 测试PartiFlow分区队列 + 所有调度策略组合
     */
    private static void testPartiFlowWithAllSchedulePolicies() throws Exception {
        log.info("\n========== 测试PartiFlow + 调度策略组合 ==========");

        // 所有Offer策略
        Map<String, OfferPolicy> offerPolicies = new HashMap<>();
        offerPolicies.put("RoundRobin", new RoundRobinOffer());
        offerPolicies.put("Random", new RandomOffer());
        offerPolicies.put("PlainHash", new PlainHashOffer());
        offerPolicies.put("BalancedHash", new BalancedHashOffer());
        offerPolicies.put("ValleyFilling", new ValleyFillingOffer());

        // 所有Poll策略
        Map<String, PollPolicy> pollPolicies = new HashMap<>();
        pollPolicies.put("RoundRobin", new RoundRobinPoll());
        pollPolicies.put("Random", new RandomPoll());
        pollPolicies.put("PeekShaving", new PeekShavingPoll());
        pollPolicies.put("ThreadBinding", new ThreadBindingPoll());

        // 所有Remove策略
        Map<String, RemovePolicy> removePolicies = new HashMap<>();
        removePolicies.put("RoundRobin", new RoundRobinRemove());
        removePolicies.put("Random", new PeekShavingRemove());
        removePolicies.put("PeekShaving", new PeekShavingRemove());

        int comboCount = 0;
        for (Map.Entry<String, OfferPolicy> op : offerPolicies.entrySet()) {
            for (Map.Entry<String, PollPolicy> pp : pollPolicies.entrySet()) {
                for (Map.Entry<String, RemovePolicy> rp : removePolicies.entrySet()) {
                    String testName = "PartiFlow[Offer=" + op.getKey() + ",Poll=" + pp.getKey() + ",Remove=" + rp.getKey() + "]";
                    testPartiFlowPartition(op.getValue(), pp.getValue(), rp.getValue(), testName);
                    comboCount++;

                    // 只测试部分组合避免测试时间过长
                    if (comboCount >= 15) {
                        log.info("  已测试 {} 个组合，继续其他测试...", comboCount);
                        return;
                    }
                }
            }
        }
    }

    /**
     * 测试PartiStill静态分区队列 + 所有调度策略组合
     */
    private static void testPartiStillWithAllSchedulePolicies() throws Exception {
        log.info("\n========== 测试PartiStill + 调度策略组合 ==========");

        Map<String, OfferPolicy> offerPolicies = new HashMap<>();
        offerPolicies.put("RoundRobin", new RoundRobinOffer());
        offerPolicies.put("Random", new RandomOffer());

        Map<String, PollPolicy> pollPolicies = new HashMap<>();
        pollPolicies.put("RoundRobin", new RoundRobinPoll());
        pollPolicies.put("Random", new RandomPoll());

        Map<String, RemovePolicy> removePolicies = new HashMap<>();
        removePolicies.put("RoundRobin", new RoundRobinRemove());
        removePolicies.put("Random", new PeekShavingRemove());

        int comboCount = 0;
        for (Map.Entry<String, OfferPolicy> op : offerPolicies.entrySet()) {
            for (Map.Entry<String, PollPolicy> pp : pollPolicies.entrySet()) {
                for (Map.Entry<String, RemovePolicy> rp : removePolicies.entrySet()) {
                    String testName = "PartiStill[Offer=" + op.getKey() + ",Poll=" + pp.getKey() + ",Remove=" + rp.getKey() + "]";
                    testPartiStillPartition(op.getValue(), pp.getValue(), rp.getValue(), testName);
                    comboCount++;
                    if (comboCount >= 6) {
                        log.info("  已测试 {} 个组合，继续其他测试...", comboCount);
                        return;
                    }
                }
            }
        }
    }

    private static void testPartiFlowPartition(OfferPolicy offer, PollPolicy poll, RemovePolicy remove, String testName) {
        String poolName = "Test_PartiFlow_" + testName.replaceAll("[\\[\\],=]", "_");
        log.info("测试: {}", testName);

        try {
            Partition<Runnable> partiFlow = new PartiFlow<>(PARTITION_NUM, QUEUE_CAPACITY, "linked", offer, poll, remove);
            WorkerFactory factory = new WorkerFactory("test-worker", true, false, 5000);
            RejectStrategy rs = new CallerRunsStrategy();

            ThreadPool pool = new ThreadPool(5, 10, poolName, factory, partiFlow, rs);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < TASK_COUNT_PER_THREAD / 2; j++) {
                            try {
                                pool.execute(() -> {
                                    try {
                                        Thread.sleep(2);
                                        totalTasksCompleted.incrementAndGet();
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                });
                                successCount.incrementAndGet();
                                totalTasksSubmitted.incrementAndGet();
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                                totalErrors.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            doneLatch.await(60, TimeUnit.SECONDS);

            Thread.sleep(1000);

            Map<Integer, Integer> partitionNums = pool.getPartitionTaskNums();
            log.info("  {} - 成功: {}, 错误: {}, 分区状态: {}", testName, successCount.get(), errorCount.get(), partitionNums);

            if (errorCount.get() > 0) {
                log.warn("  !!! {} - 出现 {} 个错误", testName, errorCount.get());
            }

            pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
            UnifiedTPRegulator.unregister(poolName);

        } catch (Exception e) {
            log.error("  !!! {} - 测试异常: {}", testName, e.getMessage(), e);
            totalErrors.incrementAndGet();
        }
    }

    private static void testPartiStillPartition(OfferPolicy offer, PollPolicy poll, RemovePolicy remove, String testName) {
        String poolName = "Test_PartiStill_" + testName.replaceAll("[\\[\\],=]", "_");
        log.info("测试: {}", testName);

        try {
            Partition<Runnable> partiStill = new PartiStill<>(PARTITION_NUM, QUEUE_CAPACITY, "linked", offer, poll, remove);
            WorkerFactory factory = new WorkerFactory("test-worker", true, false, 5000);
            RejectStrategy rs = new CallerRunsStrategy();

            ThreadPool pool = new ThreadPool(5, 10, poolName, factory, partiStill, rs);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < THREAD_COUNT; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < TASK_COUNT_PER_THREAD / 2; j++) {
                            pool.execute(() -> {
                                try {
                                    Thread.sleep(2);
                                    totalTasksCompleted.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                            successCount.incrementAndGet();
                            totalTasksSubmitted.incrementAndGet();
                        }
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            doneLatch.await(60, TimeUnit.SECONDS);

            Thread.sleep(1000);

            Map<Integer, Integer> partitionNums = pool.getPartitionTaskNums();
            log.info("  {} - 成功: {}, 分区状态: {}", testName, successCount.get(), partitionNums);

            pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
            UnifiedTPRegulator.unregister(poolName);

        } catch (Exception e) {
            log.error("  !!! {} - 测试异常: {}", testName, e.getMessage(), e);
            totalErrors.incrementAndGet();
        }
    }

    /**
     * 测试配置动态切换的并发安全性
     */
    private static void testConfigDynamicSwitch() throws Exception {
        log.info("\n========== 测试配置动态切换并发安全性 ==========");

        String poolName = "Test_ConfigSwitch";
        Partition<Runnable> queue = new LinkedBlockingQ<>(QUEUE_CAPACITY * 10);
        WorkerFactory factory = new WorkerFactory("test-worker", true, false, 5000);
        RejectStrategy rs = new DiscardStrategy();

        ThreadPool pool = new ThreadPool(10, 20, poolName, factory, queue, rs);

        CountDownLatch testStartLatch = new CountDownLatch(1);
        AtomicLong switchErrors = new AtomicLong(0);
        AtomicLong completedTasks = new AtomicLong(0);

        // 任务执行线程
        ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            taskExecutor.submit(() -> {
                try {
                    testStartLatch.await();
                    for (int j = 0; j < 1000; j++) {
                        try {
                            pool.execute(() -> {
                                try {
                                    Thread.sleep(1);
                                    completedTasks.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        } catch (Exception e) {
                            // 拒绝是正常的
                        }
                    }
                } catch (Exception e) {
                    switchErrors.incrementAndGet();
                }
            });
        }

        // 配置切换线程
        Thread configSwitchThread = new Thread(() -> {
            try {
                testStartLatch.await();
                for (int i = 0; i < 50; i++) {
                    try {
                        // 切换队列
                        Partition<Runnable> newQueue = new LinkedBlockingQ<>(QUEUE_CAPACITY * 10);
                        pool.changeQueue(newQueue, "linked");
                        configSwitchCount.incrementAndGet();

                        // 切换拒绝策略
                        RejectStrategy newRs = new CallerRunsStrategy();
                        pool.changeRejectStrategy(newRs, "callerRuns");

                        // 切换worker参数
                        pool.changeWorkerParams(8, 15, null, null, null);

                        Thread.sleep(50);
                    } catch (Exception e) {
                        log.warn("配置切换异常: {}", e.getMessage());
                        switchErrors.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                log.error("配置切换线程异常: {}", e.getMessage());
            }
        });
        configSwitchThread.start();

        testStartLatch.countDown();

        // 等待测试完成
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(30, TimeUnit.SECONDS);
        configSwitchThread.join();

        Thread.sleep(2000);

        log.info("配置切换测试 - 完成任务: {}, 配置切换: {}, 错误: {}",
                completedTasks.get(), configSwitchCount.get(), switchErrors.get());

        if (switchErrors.get() > 0) {
            log.error("!!! 配置切换测试出现 {} 个错误", switchErrors.get());
        }

        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);
    }

    /**
     * 高并发压力测试
     */
    private static void stressTestHighConcurrency() throws Exception {
        log.info("\n========== 高并发压力测试 ==========");

        String poolName = "Test_Stress";
        Partition<Runnable> queue = new LinkedBlockingQ<>(QUEUE_CAPACITY * 5);
        WorkerFactory factory = new WorkerFactory("stress-worker", true, false, 1000);
        RejectStrategy rs = new CallerRunsStrategy();

        ThreadPool pool = new ThreadPool(20, 50, poolName, factory, queue, rs);

        int stressThreads = 50;
        int tasksPerThread = 2000;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicLong totalSubmitted = new AtomicLong(0);
        AtomicLong totalCompleted = new AtomicLong(0);
        AtomicLong exceptions = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(stressThreads);
        List<Future<?>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < stressThreads; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < tasksPerThread; j++) {
                        try {
                            final int taskId = j;
                            pool.execute(() -> {
                                try {
                                    // 模拟任务执行
                                    long sum = 0;
                                    for (int k = 0; k < 100; k++) {
                                        sum += k * taskId;
                                    }
                                    totalCompleted.incrementAndGet();
                                } catch (Exception e) {
                                    // ignore
                                }
                            });
                            totalSubmitted.incrementAndGet();
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    exceptions.incrementAndGet();
                }
            });
            futures.add(future);
        }

        startLatch.countDown();

        // 等待所有任务提交完成
        for (Future<?> f : futures) {
            try {
                f.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("任务提交异常: {}", e.getMessage());
            }
        }

        // 等待队列清空
        Thread.sleep(5000);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        int remainingInQueue = pool.getTaskNums();

        log.info("高并发压力测试结果:");
        log.info("  线程数: {}, 每线程任务: {}", stressThreads, tasksPerThread);
        log.info("  总提交: {}, 总完成: {}, 异常: {}",
                totalSubmitted.get(), totalCompleted.get(), exceptions.get());
        log.info("  耗时: {}ms, 剩余队列: {}", duration, remainingInQueue);
        log.info("  吞吐量: {} 任务/秒", (totalSubmitted.get() * 1000 / duration));

        if (exceptions.get() > totalSubmitted.get() * 0.1) {
            log.error("!!! 高并发测试异常率过高: {} / {}", exceptions.get(), totalSubmitted.get());
        }

        totalTasksSubmitted.addAndGet(totalSubmitted.get());
        totalTasksCompleted.addAndGet(totalCompleted.get());
        totalErrors.addAndGet(exceptions.get());

        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        executor.shutdown();
    }

    /**
     * 创建队列实例
     */
    private static Partition<Runnable> createQueue(Class<? extends Partition> queueClass, Integer capacity) {
        try {
            if (capacity != null) {
                return queueClass.getConstructor(Integer.class).newInstance(capacity);
            } else {
                return queueClass.getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("创建队列失败: " + queueClass.getName(), e);
        }
    }

    /**
     * 创建拒绝策略实例
     */
    private static RejectStrategy createRejectStrategy(RejectStrategy original) {
        if (original instanceof CallerRunsStrategy) {
            return new CallerRunsStrategy();
        } else if (original instanceof DiscardOldestStrategy) {
            return new DiscardOldestStrategy();
        } else if (original instanceof DiscardStrategy) {
            return new DiscardStrategy();
        } else if (original instanceof AbortStrategy) {
            return new AbortStrategy();
        }
        return new DiscardStrategy();
    }
}
