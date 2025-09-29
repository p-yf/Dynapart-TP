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
    public final static String LINKED = "linked";//单链表
    public final static String PRIORITY = "priority";//优先级队列
    public final static String LINKED_S = "linkedS";//cas队列
    private final static Map<String, Class<? extends Partition>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        register(LINKED, LinkedBlockingQ.class);
        register(PRIORITY, PriorityBlockingQ.class);
        register(LINKED_S, LinkedBlockingQS.class);
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
