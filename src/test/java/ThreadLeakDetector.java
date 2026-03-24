
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.RoundRobinOffer;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.ThreadBindingPoll;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.RoundRobinRemove;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 精确的线程泄露检测测试
 * 只测试ThreadBinding策略切换队列时的线程泄露问题
 */
@Slf4j
public class ThreadLeakDetector {

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Exception {
        log.info("========== 线程泄露精确检测测试 ==========");

        // 记录初始线程
        Set<Long> initialThreadIds = new HashSet<>();
        Thread[] initialThreads = new Thread[Thread.activeCount() * 2];
        int initialCount = Thread.enumerate(initialThreads);
        for (int i = 0; i < initialCount; i++) {
            if (initialThreads[i] != null) {
                initialThreadIds.add(initialThreads[i].getId());
            }
        }
        log.info("初始线程数: {}, Thread IDs: {}", initialCount, initialThreadIds);

        // 运行测试
        testThreadBindingQueueSwitch();

        // 等待一下让线程自然结束
        Thread.sleep(5000);

        // 检查最终线程
        Set<Long> finalThreadIds = new HashSet<>();
        Thread[] finalThreads = new Thread[Thread.activeCount() * 2];
        int finalCount = Thread.enumerate(finalThreads);
        for (int i = 0; i < finalCount; i++) {
            if (finalThreads[i] != null) {
                finalThreadIds.add(finalThreads[i].getId());
            }
        }
        log.info("最终线程数: {}, Thread IDs: {}", finalCount, finalThreadIds);

        // 找出新增的线程
        Set<Long> newThreadIds = new HashSet<>(finalThreadIds);
        newThreadIds.removeAll(initialThreadIds);

        log.info("========== 线程泄露分析 ==========");
        log.info("新增线程数量: {}", newThreadIds.size());

        if (!newThreadIds.isEmpty()) {
            log.error("!!! 检测到线程泄露 - 新增线程ID:");
            for (Long tid : newThreadIds) {
                ThreadInfo info = threadMXBean.getThreadInfo(tid);
                if (info != null) {
                    log.error("  线程ID: {}, 名称: {}, 状态: {}, Stack: {}",
                            tid,
                            info.getThreadName(),
                            info.getThreadState(),
                            info.getStackTrace().length > 0 ?
                                    info.getStackTrace()[0].getClassName() + "." +
                                            info.getStackTrace()[0].getMethodName() : "N/A");
                }
            }
        } else {
            log.info("✓ 未检测到线程泄露");
        }

        // 使用ThreadMXBean精确统计
        long currentThreadCount = threadMXBean.getThreadCount();
        long peakThreadCount = threadMXBean.getPeakThreadCount();
        long totalStarted = threadMXBean.getTotalStartedThreadCount();

        log.info("========== ThreadMXBean 统计 ==========");
        log.info("当前线程数: {}", currentThreadCount);
        log.info("峰值线程数: {}", peakThreadCount);
        log.info("历史启动线程总数: {}", totalStarted);
    }

    /**
     * 核心测试：ThreadBinding + 队列切换
     */
    private static void testThreadBindingQueueSwitch() throws Exception {
        log.info("\n>>> 开始ThreadBinding队列切换测试 <<<");

        String poolName = "LeakTest_PartiFlow_TB";
        int switchCount = 50;  // 切换50次

        // 创建ThreadBinding分区队列
        Partition<Runnable> partition = new PartiFlow<>(
                4, 1000, "linked",
                new RoundRobinOffer(),
                new ThreadBindingPoll(),
                new RoundRobinRemove()
        );

        WorkerFactory factory = new WorkerFactory("leak-test-worker", true, false, 5000);
        ThreadPool pool = new ThreadPool(5, 10, poolName, factory, partition, new com.yf.core.rejectstrategy.impl.DiscardStrategy());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        AtomicLong completed = new AtomicLong(0);

        // 启动10个任务线程
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 1000; j++) {
                        pool.execute(() -> {
                            try {
                                Thread.sleep(1);
                                completed.incrementAndGet();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("任务执行异常", e);
                } finally {
                    doneLatch.countDown();
                }
            }, "TaskThread-" + i).start();
        }

        // 启动队列切换
        Thread switchThread = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < switchCount; i++) {
                    Partition<Runnable> newQueue = new PartiFlow<>(
                            4, 1000, "linked",
                            new RoundRobinOffer(),
                            new ThreadBindingPoll(),
                            new RoundRobinRemove()
                    );
                    pool.changeQueue(newQueue, "linked");
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("切换异常", e);
            }
        }, "QueueSwitchThread");
        switchThread.start();

        startLatch.countDown();

        // 等待任务完成
        doneLatch.await(60, TimeUnit.SECONDS);
        switchThread.join();

        log.info("完成任务: {}, 切换次数: {}", completed.get(), switchCount);

        // 销毁线程池
        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        log.info("线程池已销毁");
    }
}
