package com.yf.clustering.node;

import com.yf.pool.threadpool.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/20 0:17
 * @description
 */
@SpringBootApplication
@EnableScheduling
public class TaskFlowNodeStart {


    private static final int TOTAL_TASKS = 1_00_00_0; // 总任务数（够多才能体现时间差异）
    private static final int SUBMIT_THREADS = 1000; // 提交任务的线程数（建议=CPU核心数，避免线程过多）
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors(); // 线程池核心数（贴合CPU能力）


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TaskFlowNodeStart.class, args);


        // 2. 任务完成计数器（确保所有任务执行完再结束计时）
        AtomicInteger taskFinishCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis(); // 计时开始

        // 3. 用固定线程池提交任务（避免创建100个线程导致调度混乱）
        ExecutorService submitPool = Executors.newFixedThreadPool(SUBMIT_THREADS);
        int tasksPerSubmitThread = TOTAL_TASKS / SUBMIT_THREADS; // 每个提交线程负责的任务数
        for (int i = 0; i < SUBMIT_THREADS; i++) {
            submitPool.submit(() -> {
                for (int j = 0; j < tasksPerSubmitThread; j++) {
                    context.getBean(ThreadPool.class).execute(() -> {
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
        try {
            submitPool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 5. 等待线程池所有任务执行完（循环检查计数器，避免提前结束）
        while (taskFinishCount.get() < TOTAL_TASKS) {
            Thread.yield(); // 让出CPU，减少空循环消耗
        }

        // 6. 计算并输出总耗时（核心指标）
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("全部" + TOTAL_TASKS + "个任务执行完成，总耗时：" + totalTime + " 毫秒");
    }
}
