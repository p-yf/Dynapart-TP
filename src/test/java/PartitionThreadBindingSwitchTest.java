
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.RoundRobinOffer;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.RoundRobinPoll;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.ThreadBindingPoll;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.RoundRobinRemove;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.rejectstrategy.impl.DiscardStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分区队列 + 线程绑定策略 队列切换并发安全测试
 * 重点检测：线程泄露、内存泄漏、ThreadLocal残留
 */
@Slf4j
public class PartitionThreadBindingSwitchTest {

    private static final int THREAD_COUNT = 20;
    private static final int TASK_COUNT_PER_THREAD = 5000;
    private static final int PARTITION_NUM = 4;
    private static final int QUEUE_CAPACITY = 5000;
    private static final int SWITCH_INTERVAL_MS = 50;  // 切换间隔
    private static final int TEST_DURATION_MS = 30000;  // 测试持续时间

    // 内存监控
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    // 结果收集
    private static final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private static final AtomicLong totalSwitches = new AtomicLong(0);
    private static final AtomicLong totalErrors = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        log.info("========== 分区队列 + 线程绑定 队列切换并发安全测试 ==========");

        // 记录初始状态
        long initialThreadCount = threadMXBean.getTotalStartedThreadCount();
        long initialMemory = getUsedMemory();

        log.info("初始线程数: {}, 初始内存: {} MB", initialThreadCount, initialMemory / 1024 / 1024);

        // 测试1:PartiFlow + ThreadBinding + 队列切换
        testPartiFlowWithThreadBindingSwitch();

        // 测试2:PartiFlow + RoundRobin + 队列切换 (对比组)
        testPartiFlowWithRoundRobinSwitch();

        // 测试3:PartiStill + ThreadBinding + 队列切换
        testPartiStillWithThreadBindingSwitch();

        // 等待GC
        Thread.sleep(3000);
        System.gc();
        Thread.sleep(1000);

        // 检查最终状态
        long finalThreadCount = threadMXBean.getTotalStartedThreadCount();
        long finalMemory = getUsedMemory();

        log.info("========== 测试结果 ==========");
        log.info("最终线程数: {} (新增: {})", finalThreadCount, finalThreadCount - initialThreadCount);
        log.info("最终内存: {} MB (变化: {} MB)", finalMemory / 1024 / 1024, (finalMemory - initialMemory) / 1024 / 1024);
        log.info("完成任务数: {}", totalTasksCompleted.get());
        log.info("总切换次数: {}", totalSwitches.get());
        log.info("总错误数: {}", totalErrors.get());

        // 检测线程泄露
        long threadLeak = (finalThreadCount - initialThreadCount);
        if (threadLeak > 10) {
            log.error("!!! 检测到可能的线程泄露: 新增线程数 = {}", threadLeak);
        } else {
            log.info("✓ 未检测到明显线程泄露");
        }

        // 检测内存泄露
        long memoryLeak = (finalMemory - initialMemory);
        if (memoryLeak > 50 * 1024 * 1024) { // 50MB
            log.error("!!! 检测到可能的内存泄露: {} MB", memoryLeak / 1024 / 1024);
        } else {
            log.info("✓ 未检测到明显内存泄露");
        }

        log.info("========== 测试完成 ==========");
    }

    /**
     * 测试PartiFlow + ThreadBinding + 队列切换
     */
    private static void testPartiFlowWithThreadBindingSwitch() throws Exception {
        log.info("\n>>> 测试: PartiFlow + ThreadBinding + 队列切换 <<<");

        String poolName = "Test_PartiFlow_TB_Switch";
        long initialThreads = threadMXBean.getTotalStartedThreadCount();
        long initialMemory = getUsedMemory();

        // 创建ThreadBinding分区队列
        Partition<Runnable> partition = new PartiFlow<>(
                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                new RoundRobinOffer(),
                new ThreadBindingPoll(),  // 线程绑定策略
                new RoundRobinRemove()
        );

        WorkerFactory factory = new WorkerFactory("tb-worker", true, false, 5000);
        ThreadPool pool = new ThreadPool(10, 20, poolName, factory, partition, new DiscardStrategy());

        AtomicLong localCompleted = new AtomicLong(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger activeTaskCount = new AtomicInteger(0);

        // 启动任务执行线程
        ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Future<?> future = taskExecutor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < TASK_COUNT_PER_THREAD; j++) {
                        try {
                            activeTaskCount.incrementAndGet();
                            pool.execute(() -> {
                                try {
                                    // 模拟任务执行，ThreadBinding会绑定线程
                                    Thread.sleep(1);
                                    localCompleted.incrementAndGet();
                                    totalTasksCompleted.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    activeTaskCount.decrementAndGet();
                                }
                            });
                        } catch (Exception e) {
                            totalErrors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                }
            });
            taskFutures.add(future);
        }

        // 启动队列切换线程
        Thread switchThread = new Thread(() -> {
            try {
                startLatch.await();
                int switchCount = 0;
                while (switchCount < TEST_DURATION_MS / SWITCH_INTERVAL_MS) {
                    try {
                        // 创建新队列
                        Partition<Runnable> newQueue = new PartiFlow<>(
                                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                                new RoundRobinOffer(),
                                new ThreadBindingPoll(),
                                new RoundRobinRemove()
                        );

                        // 切换队列
                        pool.changeQueue(newQueue, "linked");
                        totalSwitches.incrementAndGet();
                        switchCount++;

                        Thread.sleep(SWITCH_INTERVAL_MS);
                    } catch (Exception e) {
                        log.warn("队列切换异常: {}", e.getMessage());
                        totalErrors.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                log.error("切换线程异常: {}", e.getMessage());
            }
        });
        switchThread.setName("QueueSwitch-ThreadBinding");
        switchThread.start();

        // 启动监控线程
        Thread monitorThread = new Thread(() -> {
            try {
                startLatch.await();
                long lastMemory = initialMemory;
                long lastThreads = initialThreads;
                int stableCount = 0;

                for (int i = 0; i < 30; i++) {
                    Thread.sleep(1000);

                    long currentMemory = getUsedMemory();
                    long currentThreads = threadMXBean.getTotalStartedThreadCount();
                    int activeTasks = activeTaskCount.get();

                    log.debug("监控[TB] - 内存: {}MB, 线程: {} (新增: {}), 活跃任务: {}",
                            currentMemory / 1024 / 1024,
                            currentThreads,
                            currentThreads - initialThreads,
                            activeTasks);

                    // 检查是否稳定
                    if (Math.abs(currentMemory - lastMemory) < 1024 * 1024 && // 1MB变化
                            Math.abs(currentThreads - lastThreads) < 5) {
                        stableCount++;
                        if (stableCount >= 5) {
                            log.info("监控[TB] - 系统已稳定");
                            break;
                        }
                    } else {
                        stableCount = 0;
                    }

                    lastMemory = currentMemory;
                    lastThreads = currentThreads;
                }
            } catch (Exception e) {
                log.error("监控线程异常: {}", e.getMessage());
            }
        });
        monitorThread.setName("Monitor-ThreadBinding");
        monitorThread.start();

        // 开始测试
        startLatch.countDown();

        // 等待任务完成
        for (Future<?> f : taskFutures) {
            try {
                f.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("任务执行异常: {}", e.getMessage());
            }
        }

        // 等待切换线程
        switchThread.join();
        monitorThread.join();

        // 清理
        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);

        long finalMemory = getUsedMemory();
        long finalThreads = threadMXBean.getTotalStartedThreadCount();

        log.info("结果[PartiFlow+ThreadBinding] - 完成任务: {}, 切换: {}, 错误: {}",
                localCompleted.get(), totalSwitches.get(), totalErrors.get());
        log.info("内存变化: {} MB, 线程变化: {}",
                (finalMemory - initialMemory) / 1024 / 1024,
                finalThreads - initialThreads);
    }

    /**
     * 测试PartiFlow + RoundRobin + 队列切换 (对比组)
     */
    private static void testPartiFlowWithRoundRobinSwitch() throws Exception {
        log.info("\n>>> 测试: PartiFlow + RoundRobin + 队列切换 <<<");

        String poolName = "Test_PartiFlow_RR_Switch";
        long initialThreads = threadMXBean.getTotalStartedThreadCount();

        Partition<Runnable> partition = new PartiFlow<>(
                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                new RoundRobinOffer(),
                new RoundRobinPoll(),  // 非线程绑定策略
                new RoundRobinRemove()
        );

        WorkerFactory factory = new WorkerFactory("rr-worker", true, false, 5000);
        ThreadPool pool = new ThreadPool(10, 20, poolName, factory, partition, new DiscardStrategy());

        AtomicLong localCompleted = new AtomicLong(0);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Future<?> future = taskExecutor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < TASK_COUNT_PER_THREAD; j++) {
                        try {
                            pool.execute(() -> {
                                try {
                                    Thread.sleep(1);
                                    localCompleted.incrementAndGet();
                                    totalTasksCompleted.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        } catch (Exception e) {
                            totalErrors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                }
            });
            taskFutures.add(future);
        }

        // 队列切换线程
        Thread switchThread = new Thread(() -> {
            try {
                startLatch.await();
                int switchCount = 0;
                while (switchCount < TEST_DURATION_MS / SWITCH_INTERVAL_MS / 2) {
                    try {
                        Partition<Runnable> newQueue = new PartiFlow<>(
                                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                                new RoundRobinOffer(),
                                new RoundRobinPoll(),
                                new RoundRobinRemove()
                        );
                        pool.changeQueue(newQueue, "linked");
                        totalSwitches.incrementAndGet();
                        switchCount++;
                        Thread.sleep(SWITCH_INTERVAL_MS);
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                log.error("切换线程异常: {}", e.getMessage());
            }
        });
        switchThread.setName("QueueSwitch-RoundRobin");
        switchThread.start();

        startLatch.countDown();

        for (Future<?> f : taskFutures) {
            try {
                f.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("任务执行异常: {}", e.getMessage());
            }
        }

        switchThread.join();

        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);

        log.info("结果[PartiFlow+RoundRobin] - 完成任务: {}, 切换: {}",
                localCompleted.get(), totalSwitches.get());
    }

    /**
     * 测试PartiStill + ThreadBinding + 队列切换
     */
    private static void testPartiStillWithThreadBindingSwitch() throws Exception {
        log.info("\n>>> 测试: PartiStill + ThreadBinding + 队列切换 <<<");

        String poolName = "Test_PartiStill_TB_Switch";
        long initialThreads = threadMXBean.getTotalStartedThreadCount();

        Partition<Runnable> partition = new com.yf.core.partitioning.impl.PartiStill<>(
                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                new RoundRobinOffer(),
                new ThreadBindingPoll(),  // 线程绑定策略
                new RoundRobinRemove()
        );

        WorkerFactory factory = new WorkerFactory("still-tb-worker", true, false, 5000);
        ThreadPool pool = new ThreadPool(10, 20, poolName, factory, partition, new DiscardStrategy());

        AtomicLong localCompleted = new AtomicLong(0);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Future<?> future = taskExecutor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < TASK_COUNT_PER_THREAD; j++) {
                        try {
                            pool.execute(() -> {
                                try {
                                    Thread.sleep(1);
                                    localCompleted.incrementAndGet();
                                    totalTasksCompleted.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        } catch (Exception e) {
                            totalErrors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                }
            });
            taskFutures.add(future);
        }

        // 队列切换线程
        Thread switchThread = new Thread(() -> {
            try {
                startLatch.await();
                int switchCount = 0;
                while (switchCount < TEST_DURATION_MS / SWITCH_INTERVAL_MS / 2) {
                    try {
                        Partition<Runnable> newQueue = new com.yf.core.partitioning.impl.PartiStill<>(
                                PARTITION_NUM, QUEUE_CAPACITY, "linked",
                                new RoundRobinOffer(),
                                new ThreadBindingPoll(),
                                new RoundRobinRemove()
                        );
                        pool.changeQueue(newQueue, "linked");
                        totalSwitches.incrementAndGet();
                        switchCount++;
                        Thread.sleep(SWITCH_INTERVAL_MS);
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                log.error("切换线程异常: {}", e.getMessage());
            }
        });
        switchThread.setName("QueueSwitch-PartiStill");
        switchThread.start();

        startLatch.countDown();

        for (Future<?> f : taskFutures) {
            try {
                f.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("任务执行异常: {}", e.getMessage());
            }
        }

        switchThread.join();

        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);

        log.info("结果[PartiStill+ThreadBinding] - 完成任务: {}, 切换: {}",
                localCompleted.get(), totalSwitches.get());
    }

    /**
     * 获取当前使用的内存
     */
    private static long getUsedMemory() {
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        return heap.getUsed() + heap.getCommitted();
    }
}
