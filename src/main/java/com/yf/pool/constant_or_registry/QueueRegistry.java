package com.yf.pool.constant_or_registry;

import com.yf.pool.partition.Impl.LinkedBlockingQ;
import com.yf.pool.partition.Impl.LinkedBlockingQS;
import com.yf.pool.partition.Impl.PriorityBlockingQ;
import com.yf.pool.partition.Partition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description
 */
public class QueueRegistry {
    //这个mini队列之所以没被删除，因为这是本项目的第一个任务队列，也是我实现的第一个阻塞队列，已经对它产生感情了
    public final static String LINKED = "linked";//单链表
    public final static String PRIORITY = "priority";//优先级队列
    public final static String LINKED_S = "linkedS";
    public final static Map<String, Class<? extends Partition>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQ.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQ.class);
        TASK_QUEUE_MAP.put(LINKED_S, LinkedBlockingQS.class);
    }

    public static void register(String name, Class<? extends Partition> clazz) {
        TASK_QUEUE_MAP.put(name, clazz);
    }
}
