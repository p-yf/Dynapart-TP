package com.yf.core.resource_manager;

import com.yf.common.constant.OfPool;
import com.yf.common.task.GCTask;
import com.yf.common.task.impl.TBPollCleaningTask;
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.Partitioning;
import com.yf.core.partitioning.schedule_policy.SchedulePolicy;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.ThreadBindingPoll;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.workerfactory.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @date 2025/10/6 14:57
 * @description 用来垃圾清理的以及异步操作的小线程池
 *
 * 虽然在利用了全局锁+标记+抛出异常三个步骤能解决大部分问题，但是依旧可能存在一部分无法优雅解决的问题，例如：
 * 1 ThreadBinding策略的ThreadLocal对应的value无法被正确回收，会造成内存泄漏问题。（也有其他解决方案，只是这种更加的具有扩展性）
 * 2 倘若有入队队列用无锁操作，那么就会出现旧队列元素残留问题，用这种方式能够最大限度的解决（不能说100%）
 */
@Slf4j
public class GCTaskManager {
    private static volatile ThreadPool littleChief;
    private final static Map<Class<? extends SchedulePolicy>,Class<? extends GCTask>> SCHEDULE_TASK_MAP = new HashMap<>();
    private final static Map<Class<? extends Partition<?>>,Class<? extends GCTask>> PARTI_TASK_MAP = new HashMap<>();
    static {
        register(ThreadBindingPoll.class, TBPollCleaningTask.class);
    }
    public static void register(Class resourceClazz, Class<? extends GCTask> taskClass){
        if(resourceClazz.getSuperclass() == SchedulePolicy.class) {
            SCHEDULE_TASK_MAP.put(resourceClazz, taskClass);
        }
        if(resourceClazz.getSuperclass() == Partition.class){
            PARTI_TASK_MAP.put(resourceClazz, taskClass);
        }
    }

    public static void clean(ThreadPool tp, Partition<?> partition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if(partition instanceof Partitioning<?>){//调度规则相关的清理策略
            //获取清理任务
            GCTask offerTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getOfferPolicy().getClass())
                    .getConstructor(ThreadPool.class).newInstance().build(tp);
            GCTask pollTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getPollPolicy().getClass())
                    .getConstructor(ThreadPool.class).newInstance(tp).build(tp);
            GCTask removeTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getRemovePolicy().getClass())
                    .getConstructor(ThreadPool.class).newInstance(tp).build(tp);

            //执行
            execute(offerTask);
            execute(pollTask);
            execute(removeTask);
        }else{//分区相关的清理策略
            GCTask task = PARTI_TASK_MAP.get(partition.getClass())
                    .getConstructor(ThreadPool.class).newInstance(tp);
            execute(task);
        }
    }

    public static void execute(Runnable task){
        if(task == null){
            return;
        }
        if (littleChief == null) {
            synchronized (GCTaskManager.class) {
                if (littleChief == null) {
                    littleChief = new ThreadPool(
                            OfPool.LITTLE_CHIEF,
                            5,
                            10,
                            OfPool.LITTLE_CHIEF,
                            new WorkerFactory("", false, true, 10),
                            new LinkedBlockingQ<Runnable>(50),
                            new CallerRunsStrategy()
                    );
                }
            }
        }

        littleChief.execute(task);
    }

    public static synchronized void setLittleChief(ThreadPool tp) {
        if(littleChief!=null) return;

        littleChief = tp;
    }
}
