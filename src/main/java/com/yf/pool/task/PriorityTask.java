package com.yf.pool.task;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author yyf
 * @date 2025/8/2 19:31
 * @description
 */
@Setter
@Getter
public class PriorityTask extends FutureTask implements Comparator<PriorityTask> {
    private int priority = 0;

    public PriorityTask(Callable callable,int priority) {
        super(callable);
        this.priority = priority;
    }

    public PriorityTask(Runnable runnable, Object result,int priority) {
        super(runnable, result);
        this.priority = priority;
    }

    public int compare(PriorityTask t1, PriorityTask t2) {
        return t2.priority - t1.priority;
    }
}
