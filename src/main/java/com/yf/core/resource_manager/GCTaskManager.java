package com.yf.core.resource_manager;

import com.yf.common.constant.Logo;
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
 * @description 用来垃圾清理的线程池
 */
@Slf4j
public class GCTaskManager {
    private static volatile ThreadPool littleChief;
    private final static Map<Class<? extends SchedulePolicy>,Class<? extends GCTask>> SCHEDULE_TASK_MAP = new HashMap<>();
    private final static Map<Class<? extends Partition<?>>,Class<? extends GCTask>> PARTI_TASK_MAP = new HashMap<>();
    static {
        register(ThreadBindingPoll.class, TBPollCleaningTask.class);
    }
    public static void register(Class clazz,Class<? extends GCTask> taskClass){
        if(clazz.getSuperclass() == SchedulePolicy.class) {
            SCHEDULE_TASK_MAP.put(clazz, taskClass);
        }
        if(clazz.getSuperclass() == Partition.class){
            PARTI_TASK_MAP.put(clazz, taskClass);
        }
    }

    public static void clean(ThreadPool tp, Partition<?> partition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if(partition instanceof Partitioning<?>){
            GCTask offerTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getOfferPolicy().getClass()).getConstructor(ThreadPool.class).newInstance(tp);
            execute(offerTask);
            GCTask pollTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getPollPolicy().getClass()).getConstructor(ThreadPool.class).newInstance(tp);
            execute(pollTask);
            GCTask removeTask = SCHEDULE_TASK_MAP.get(((Partitioning<?>) partition).getRemovePolicy().getClass()).getConstructor(ThreadPool.class).newInstance(tp);
            execute(removeTask);
        }else{
            GCTask task = PARTI_TASK_MAP.get(partition.getClass()).getConstructor(ThreadPool.class).newInstance(tp);
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
                            5,
                            10,
                            "GC-ThreadPool",
                            new WorkerFactory("", false, true, 10),
                            new LinkedBlockingQ<Runnable>(50),
                            new CallerRunsStrategy()
                    );
                    log.info(Logo.LOG_LOGO+"GC线程池已成功启动");
                }
            }
        }

        littleChief.execute(task);
    }
}
