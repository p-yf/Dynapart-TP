import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.taskqueue.Impl.PartiFlowTaskQ;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.RemoveStrategy;
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
    static AtomicInteger count = new AtomicInteger(0);
    public static void main(String[] args) {
        testMine();
//        testJDK();
    }

    private static void testMine() {
        ThreadPool threadPool = new ThreadPool(
                10,
                20,
                "",
                new ThreadFactory("MineThreadPool", false, false, 5000),
                new PartiFlowTaskQ(10,1000, OfferStrategy.HASH, PollStrategy.ROUND_ROBIN, RemoveStrategy.ROUND_ROBIN),
                new CallerRunsStrategy()
        );
        long timeMillis = System.currentTimeMillis();
        threadPool.execute(()->{
            for(int i = 0; i < 20000000; i++) {
                threadPool.execute(() -> {
                    System.out.println(System.currentTimeMillis() - timeMillis);
                    System.err.println(count.incrementAndGet());
                });
            }
        });


    }

    private static void testJDK() {
        int corePoolSize = 10;          // 核心线程数
        int maximumPoolSize = 20;      // 最大线程数
        long keepAliveTime = 5;        // 空闲线程存活时间
        TimeUnit unit = TimeUnit.SECONDS; // 时间单位

        // 使用链表阻塞队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(10000);

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

        long timeMillis = System.currentTimeMillis();
        threadPool.execute(()->{
            for(int i = 0; i < 20000000; i++) {
                threadPool.execute(() -> {
                    System.out.println(System.currentTimeMillis() - timeMillis);
                    System.err.println(count.incrementAndGet());
                });
            }
        });
    }
}
