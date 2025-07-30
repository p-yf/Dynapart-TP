package com.yf.pool.constant;

import com.yf.pool.taskqueue.Impl.LinkedBlockingQueue;

import java.util.HashMap;
import java.util.Map;

public class OfQueue {
    public final static String LINKED = "linked";//链表
    public static Map<String, Class<?>> TASK_QUEUE_MAP = new HashMap<>();
    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQueue.class);
    }
}
