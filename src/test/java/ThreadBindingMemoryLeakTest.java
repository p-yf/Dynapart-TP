
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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 长时间高频率队列切换 - 专门检测ThreadBinding的ThreadLocal泄露问题
 */
@Slf4j
public class ThreadBindingMemoryLeakTest {

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public static void main(String[] args) throws Exception {
        log.info("========== ThreadBinding ThreadLocal泄露检测 ==========");

        // 记录初始状态
        long initialThreadCount = threadMXBean.getThreadCount();
        long initialMemory = getHeapUsed();
        log.info("初始状态 - 线程: {}, 堆内存: {} MB", initialThreadCount, initialMemory / 1024 / 1024);

        // 创建一个线程池使用ThreadBinding
        String poolName = "ThreadBindingLeakTest";
        Partition<Runnable> partition = new PartiFlow<>(
                4, 5000, "linked",
                new RoundRobinOffer(),
                new ThreadBindingPoll(),
                new RoundRobinRemove()
        );

        WorkerFactory factory = new WorkerFactory("tb-leak-worker", true, false, 5000);
        ThreadPool pool = new ThreadPool(10, 20, poolName, factory, partition, new com.yf.core.rejectstrategy.impl.CallerRunsStrategy());

        AtomicLong totalCompleted = new AtomicLong(0);
        int round = 0;

        // 进行多轮测试，每轮创建新队列并切换
        for (int i = 0; i < 10; i++) {
            round++;
            log.info("\n=== 第 {} 轮测试 ===", round);

            long roundStartMemory = getHeapUsed();
            long roundStartThreads = threadMXBean.getThreadCount();

            // 每轮使用新的CountDownLatch
            CountDownLatch roundLatch = new CountDownLatch(1);

            // 启动任务线程
            ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < 10; t++) {
                final CountDownLatch latchForTask = roundLatch;
                Future<?> f = taskExecutor.submit(() -> {
                    try {
                        latchForTask.await();
                        for (int j = 0; j < 5000; j++) {
                            pool.execute(() -> {
                                try {
                                    Thread.sleep(1);
                                    totalCompleted.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error("任务异常", e);
                    }
                });
                futures.add(f);
            }

            roundLatch.countDown();

            // 高频率切换队列 (每50ms切换一次)
            Thread switchThread = new Thread(() -> {
                try {
                    for (int s = 0; s < 100; s++) { // 每轮100次切换
                        Partition<Runnable> newQueue = new PartiFlow<>(
                                4, 5000, "linked",
                                new RoundRobinOffer(),
                                new ThreadBindingPoll(),
                                new RoundRobinRemove()
                        );
                        pool.changeQueue(newQueue, "linked");
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    log.error("切换异常", e);
                }
            });
            switchThread.start();

            // 等待任务和切换完成
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
            switchThread.join();
            taskExecutor.shutdown();
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);

            long roundEndMemory = getHeapUsed();
            long roundEndThreads = threadMXBean.getThreadCount();

            log.info("第 {} 轮完成 - 完成任务: {}, 切换: 100", round, totalCompleted.get());
            log.info("  内存变化: {} MB", (roundEndMemory - roundStartMemory) / 1024 / 1024);
            log.info("  线程变化: {}", (roundEndThreads - roundStartThreads));
        }

        // 销毁线程池
        pool.destroyWorkers(pool.getCoreNums(), pool.getMaxNums() - pool.getCoreNums());
        UnifiedTPRegulator.unregister(poolName);

        // 等待GC
        Thread.sleep(2000);
        System.gc();
        Thread.sleep(1000);

        // 检查最终状态
        long finalThreadCount = threadMXBean.getThreadCount();
        long finalMemory = getHeapUsed();
        long peakThreads = threadMXBean.getPeakThreadCount();
        long totalStarted = threadMXBean.getTotalStartedThreadCount();

        log.info("\n========== 测试结果 ==========");
        log.info("总完成任务: {}", totalCompleted.get());
        log.info("总切换次数: {}", round * 100);
        log.info("初始状态 - 线程: {}, 堆内存: {} MB", initialThreadCount, initialMemory / 1024 / 1024);
        log.info("最终状态 - 线程: {}, 堆内存: {} MB", finalThreadCount, finalMemory / 1024 / 1024);
        log.info("峰值线程数: {}", peakThreads);
        log.info("历史启动线程总数: {}", totalStarted);

        // 判断泄露
        long threadLeak = finalThreadCount - initialThreadCount;
        long memoryLeak = finalMemory - initialMemory;

        log.info("\n========== 泄露分析 ==========");
        if (threadLeak > 5) {
            log.error("!!! 可能存在线程泄露: 增加 {} 个线程", threadLeak);
        } else {
            log.info("✓ 线程数量正常");
        }

        if (memoryLeak > 10 * 1024 * 1024) { // 10MB
            log.error("!!! 可能存在内存泄露: 增加 {} MB", memoryLeak / 1024 / 1024);
        } else {
            log.info("✓ 内存使用正常");
        }

        // 分析ThreadBindingPoll的ThreadLocal
        log.info("\n========== ThreadBinding ThreadLocal分析 ==========");
        log.info("ThreadBindingPoll使用ThreadLocal存储线程绑定信息");
        log.info("队列切换时，通过GCTaskManager执行DefaultPartitionGCTask清理");
        log.info("如果清理不彻底，可能导致ThreadLocal值对象无法回收");

        // 检查当前线程的ThreadLocal
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);
        Map<String, Object> threadLocalSummary = new HashMap<>();

        for (int i = 0; i < count; i++) {
            if (threads[i] != null) {
                // 这里只是记录，实际的ThreadLocal值无法直接访问
                threadLocalSummary.put(threads[i].getName(), threads[i].getThreadGroup().getName());
            }
        }
        log.info("当前活跃线程组: {}", threadLocalSummary.values());
    }

    private static long getHeapUsed() {
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        return heap.getUsed();
    }
}
