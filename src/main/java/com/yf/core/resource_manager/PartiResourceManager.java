package com.yf.core.resource_manager;

import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Impl.LinkedBlockingQS;
import com.yf.core.partition.Impl.PriorityBlockingQ;
import com.yf.core.partition.Partition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description: 分区资源管理器(PartitionResourceManager)
 */
public class PartiResourceManager {
    //这个mini队列之所以没被删除，因为这是本项目的第一个任务队列，也是我实现的第一个阻塞队列，已经对它产生感情了
    public final static String LINKED = "linked";//单链表
    public final static String PRIORITY = "priority";//优先级队列
    public final static String LINKED_S = "linkedS";//cas队列
    private final static Map<String, Class<? extends Partition>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQ.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQ.class);
        TASK_QUEUE_MAP.put(LINKED_S, LinkedBlockingQS.class);
    }

    public static void register(String name, Class<? extends Partition> clazz) {
        TASK_QUEUE_MAP.put(name, clazz);
    }

    public static Class<? extends Partition> getResource(String name) {
        return TASK_QUEUE_MAP.get(name);
    }

    public static Map<String,Class<? extends Partition>> getResources(){
        return TASK_QUEUE_MAP;
    }
}
