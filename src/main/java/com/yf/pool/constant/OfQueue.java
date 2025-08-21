package com.yf.pool.constant;

import com.yf.pool.partition.Impl.LinkedBlockingQMini;
import com.yf.pool.partition.Impl.LinkedBlockingQPlus;
import com.yf.pool.partition.Impl.PriorityBlockingQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description
 */
public class OfQueue {
    //这个mini队列之所以没被删除，因为这是本项目的第一个任务队列，也是我实现的第一个阻塞队列，已经对它产生感情了
    public final static String LINKED_MINI = "linked_mini";
    public final static String LINKED_PLUS = "linked_plus";//单链表
    public final static String LINKED_PRO = "linked_pro";
    public final static String PRIORITY = "priority";//优先级队列
    public final static Map<String, Class<?>> TASK_QUEUE_MAP = new HashMap<>();

    static {
        TASK_QUEUE_MAP.put(LINKED_MINI, LinkedBlockingQMini.class);
        TASK_QUEUE_MAP.put(LINKED_PLUS, LinkedBlockingQPlus.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQueue.class);
        TASK_QUEUE_MAP.put(LINKED_PRO, LinkedBlockingQPlus.class);
    }
}
