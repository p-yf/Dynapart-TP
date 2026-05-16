package com.yf.test;

import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Impl.LinkedBlockingQS;
import com.yf.core.partition.Impl.PriorityBlockingQ;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.impl.PartiStill;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.RoundRobinOffer;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.RoundRobinPoll;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.RoundRobinRemove;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;
import com.yf.core.rejectstrategy.impl.DiscardStrategy;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 队列切换线程泄露测试
 * 验证当队列在运行过程中切换时，等待中的消费者线程能够正确感知切换并迁移到新队列
 */
@Slf4j
public class QueueSwitchThreadLeakTest {

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Exception {
        log.info("========== 队列切换线程泄露测试 ==========");

        // 记录初始线程数
        long initialThreadCount = threadMXBean.getThreadCount();
        log.info("初始线程数: {}", initialThreadCount);

        // 测试所有队列类型
        testAllQueueTypes();

        // 等待清理
        Thread.sleep(3000);

        // 检查最终线程数
        long finalThreadCount = threadMXBean.getThreadCount();
        log.info("最终线程数: {}", finalThreadCount);

        if (finalThreadCount > initialThreadCount + 5) {
            log.error("!!! 检测到线程泄露！初始: {}, 最终: {}", initialThreadCount, finalThreadCount);
            System.exit(1);
        } else {
            log.info("✓ 测试通过，未检测到线程泄露");
        }
    }

    /**
     * 测试所有队列类型
     */
    private static void testAllQueueTypes() throws Exception {
        // 测试单个队列实现
        testQueueType("LinkedBlockingQ", () -> new LinkedBlockingQ<>(1000));
        testQueueType("LinkedBlockingQS", () -> new LinkedBlockingQS<>(1000));
        testQueueType("PriorityBlockingQ", () -> new PriorityBlockingQ<>(1000));

        // 测试分区队列
        testPartitionQueueType("PartiFlow_Linked", () -> new PartiFlow<>(4, 1000, "linked",
                new RoundRobinOffer(), new RoundRobinPoll(), new RoundRobinRemove()));
        testPartitionQueueType("PartiStill_Linked", () -> new PartiStill<>(4, 1000, "linked",
                new RoundRobinOffer(), new RoundRobinPoll(), new RoundRobinRemove()));
    }

    /**
     * 测试单个队列类型
     */
    private static void testQueueType(String queueName, java.util.function.Supplier<Partition<Runnable>> queueSupplier) throws Exception {
        log.info("\n>>> 测试队列: {} <<<", queueName);

        String poolName = "TestPool_" + queueName;
        Partition<Runnable> partition = queueSupplier.get();

        WorkerFactory factory = new WorkerFactory("worker-" + queueName, true, false, 5000);
        ThreadPool pool = new ThreadPool(3, 5, poolName, factory, partition, new DiscardStrategy());

        // 启动消费者线程，这些线程会在空队列上等待
        CountDownLatch consumerReadyLatch = new CountDownLatch(3);
        AtomicBoolean consumersRunning = new AtomicBoolean(true);
        AtomicInteger switchCount = new AtomicInteger(0);

        // 启动消费者线程，它们会阻塞在 poll() 上
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    consumerReadyLatch.countDown();
                    while (consumersRunning.get()) {
                        try {
                            Runnable task = pool.getPartition().poll(500);
                            if (task != null) {
                                task.run();
                            }
                        } catch (Exception e) {
                            // SwitchedException 会导致 poll 返回 null，继续循环即可
                            Thread.sleep(10);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + queueName + "-" + threadId).start();
        }

        // 等待消费者准备就绪
        consumerReadyLatch.await();
        Thread.sleep(500);

        // 执行队列切换
        for (int i = 0; i < 10; i++) {
            Partition<Runnable> newQueue = queueSupplier.get();
            UnifiedTPRegulator.changeQueue(poolName, newQueue);
            switchCount.incrementAndGet();
            Thread.sleep(200);
        }

        // 停止消费者
        consumersRunning.set(false);
        Thread.sleep(500);

        // 销毁线程池
        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        log.info("队列 {} 切换 {} 次完成", queueName, switchCount.get());
    }

    /**
     * 测试分区队列类型
     */
    private static void testPartitionQueueType(String queueName, java.util.function.Supplier<Partition<Runnable>> queueSupplier) throws Exception {
        log.info("\n>>> 测试分区队列: {} <<<", queueName);

        String poolName = "TestPool_" + queueName;
        Partition<Runnable> partition = queueSupplier.get();

        WorkerFactory factory = new WorkerFactory("worker-" + queueName, true, false, 5000);
        ThreadPool pool = new ThreadPool(4, 8, poolName, factory, partition, new DiscardStrategy());

        CountDownLatch consumerReadyLatch = new CountDownLatch(4);
        AtomicBoolean consumersRunning = new AtomicBoolean(true);
        AtomicInteger switchCount = new AtomicInteger(0);

        // 启动消费者线程
        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    consumerReadyLatch.countDown();
                    while (consumersRunning.get()) {
                        try {
                            Runnable task = pool.getPartition().poll(500);
                            if (task != null) {
                                task.run();
                            }
                        } catch (Exception e) {
                            Thread.sleep(10);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + queueName + "-" + threadId).start();
        }

        consumerReadyLatch.await();
        Thread.sleep(500);

        // 执行队列切换
        for (int i = 0; i < 10; i++) {
            Partition<Runnable> newQueue = queueSupplier.get();
            UnifiedTPRegulator.changeQueue(poolName, newQueue);
            switchCount.incrementAndGet();
            Thread.sleep(200);
        }

        consumersRunning.set(false);
        Thread.sleep(500);

        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        log.info("分区队列 {} 切换 {} 次完成", queueName, switchCount.get());
    }
}