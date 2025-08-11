package com.yf.pool.constant;

import com.yf.pool.taskqueue.Impl.LinkedBlockingQueue;
import com.yf.pool.taskqueue.Impl.PartiFlowTaskQ;
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
    public final static String PARTI_FLOW = "parti_flow";//分区流队列
    public final static Map<String, Class<?>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQueue.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQueue.class);
        TASK_QUEUE_MAP.put(PARTI_FLOW, PartiFlowTaskQ.class);
    }
}
