package com.yf.pool.constant;

import com.yf.pool.taskqueue.Impl.LinkedBlockingQueue;
import com.yf.pool.taskqueue.Impl.PriorityBlockingQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description
 */
public class OfQueue {
    public final static String LINKED = "linked";//单链表
    public final static String PRIORITY = "priority";//优先级队列
    public final static Map<String, Class<?>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQueue.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQueue.class);
    }
}
