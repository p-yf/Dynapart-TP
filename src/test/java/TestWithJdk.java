import com.yf.pool.partition.Impl.parti_flow.PartiFlow;
import com.yf.pool.partition.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.RemoveStrategy;
import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.partition.Impl.LinkedBlockingQMini;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.threadpool.ThreadPool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/10 16:41
 * @description
 */

public class TestWithJdk {
    // 可调整参数
    private static final int TOTAL_TASKS = 1_00_000_0; // 总任务数（够多才能体现时间差异）
    private static final int SUBMIT_THREADS = 10000; // 提交任务的线程数（建议=CPU核心数，避免线程过多）
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors(); // 线程池核心数（贴合CPU能力）

    public static void main(String[] args) throws InterruptedException {
        testMineParti();//22748  16676 毫秒  20337  22191  22812 24749 24474
//        testMyLBQ();
//        testJDK();//23550  17337 毫秒  20461  21840 23471 23433 26009

    }

    private static void testMineParti() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(
                0,
                20,
                "",
                new ThreadFactory("", false, false, 2000),
                new PartiFlow(10,5001, "linked_plus" , OfferStrategy.HASH, PollStrategy.THREAD_BINDING, RemoveStrategy.ROUND_ROBIN),
//                new LinkedBlockingQueuePlus(5000),
//                new PriorityBlockingQueue(5000),
                new CallerRunsStrategy()
        );
        // 2. 任务完成计数器（确保所有任务执行完再结束计时）
        AtomicInteger taskFinishCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis(); // 计时开始

        // 3. 用固定线程池提交任务（避免创建100个线程导致调度混乱）
        ExecutorService submitPool = Executors.newFixedThreadPool(SUBMIT_THREADS);
        int tasksPerSubmitThread = TOTAL_TASKS / SUBMIT_THREADS; // 每个提交线程负责的任务数

        for (int i = 0; i < SUBMIT_THREADS; i++) {
            submitPool.submit(() -> {
                for (int j = 0; j < tasksPerSubmitThread; j++) {
                    threadPool.execute(() -> {
                        // 模拟真实任务：简单计算（避免JIT优化掉空任务）
                        long val = (System.nanoTime() * 3 + 7) % 11;
                        // 任务完成，计数器+1
                        System.out.println(val);
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println(val);
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        taskFinishCount.incrementAndGet();

                    });
                }
            });
        }

        // 4. 等待所有任务提交完，关闭提交池
        submitPool.shutdown();
        submitPool.awaitTermination(1, TimeUnit.MINUTES);

        // 5. 等待线程池所有任务执行完（循环检查计数器，避免提前结束）
        while (taskFinishCount.get() < TOTAL_TASKS) {
            Thread.yield(); // 让出CPU，减少空循环消耗
        }

        // 6. 计算并输出总耗时（核心指标）
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("全部" + TOTAL_TASKS + "个任务执行完成，总耗时：" + totalTime + " 毫秒");

    }



    private static void testJDK() throws InterruptedException {
        int corePoolSize = 0;          // 核心线程数
        int maximumPoolSize = 20;      // 最大线程数
        long keepAliveTime = 2;        // 空闲线程存活时间
        TimeUnit unit = TimeUnit.SECONDS; // 时间单位

        // 使用链表阻塞队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(5000);

        // 线程工厂 - 用于创建新线程
        java.util.concurrent.ThreadFactory threadFactory = Executors.defaultThreadFactory();

        // 拒绝策略 - 使用CallerRunsPolicy，让提交任务的线程自己执行任务
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        // 创建线程池
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
        // 2. 任务完成计数器（确保所有任务执行完再结束计时）
        AtomicInteger taskFinishCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis(); // 计时开始

        // 3. 用固定线程池提交任务（避免创建100个线程导致调度混乱）
        ExecutorService submitPool = Executors.newFixedThreadPool(SUBMIT_THREADS);
        int tasksPerSubmitThread = TOTAL_TASKS / SUBMIT_THREADS; // 每个提交线程负责的任务数

        for (int i = 0; i < SUBMIT_THREADS; i++) {
            submitPool.submit(() -> {
                for (int j = 0; j < tasksPerSubmitThread; j++) {
                    threadPool.execute(() -> {
                        // 模拟真实任务：简单计算（避免JIT优化掉空任务）
                        long val = (System.nanoTime() * 3 + 7) % 11;
                        // 任务完成，计数器+1
                        System.out.println(val);
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println(val);
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        System.out.println("doit");
                        taskFinishCount.incrementAndGet();

                    });
                }
            });
        }

        // 4. 等待所有任务提交完，关闭提交池
        submitPool.shutdown();
        submitPool.awaitTermination(1, TimeUnit.MINUTES);

        // 5. 等待线程池所有任务执行完（循环检查计数器，避免提前结束）
        while (taskFinishCount.get() < TOTAL_TASKS) {
            Thread.yield(); // 让出CPU，减少空循环消耗
        }

        // 6. 计算并输出总耗时（核心指标）
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("全部" + TOTAL_TASKS + "个任务执行完成，总耗时：" + totalTime + " 毫秒");



            }

    private static void testMyLBQ() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(
                10,
                10,
                "",
                new ThreadFactory("", false, false, 2000),
                new LinkedBlockingQMini(5000),
                new CallerRunsStrategy()
        );
        // 2. 任务完成计数器（确保所有任务执行完再结束计时）
        AtomicInteger taskFinishCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis(); // 计时开始

        // 3. 用固定线程池提交任务（避免创建100个线程导致调度混乱）
        ExecutorService submitPool = Executors.newFixedThreadPool(SUBMIT_THREADS);
        int tasksPerSubmitThread = TOTAL_TASKS / SUBMIT_THREADS; // 每个提交线程负责的任务数

        for (int i = 0; i < SUBMIT_THREADS; i++) {
            submitPool.submit(() -> {
                for (int j = 0; j < tasksPerSubmitThread; j++) {
                    threadPool.execute(() -> {
                        // 模拟真实任务：简单计算（避免JIT优化掉空任务）
                        long val = (System.nanoTime() * 3 + 7) % 11;
                        // 任务完成，计数器+1
                        taskFinishCount.incrementAndGet();
                        System.out.println(val);
                        System.out.println("doit");
                    });
                }
            });
        }

        // 4. 等待所有任务提交完，关闭提交池
        submitPool.shutdown();
        submitPool.awaitTermination(1, TimeUnit.MINUTES);

        // 5. 等待线程池所有任务执行完（循环检查计数器，避免提前结束）
        while (taskFinishCount.get() < TOTAL_TASKS) {
            Thread.yield(); // 让出CPU，减少空循环消耗
        }

        // 6. 计算并输出总耗时（核心指标）
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("全部" + TOTAL_TASKS + "个任务执行完成，总耗时：" + totalTime + " 毫秒");

    }

}
