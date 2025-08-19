package com.yf.pool.task;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author yyf
 * @date 2025/8/2 19:31
 * @description 带有优先级的任务类
 */
@Setter
@Getter
public class PriorityTask extends FutureTask implements Comparable<PriorityTask> {
    private int priority = 0;

    public PriorityTask(Callable<?> callable, int priority) {
        super(callable);
        this.priority = priority;
    }

    public PriorityTask(Runnable runnable, Object result, int priority) {
        super(runnable, result);
        this.priority = priority;
    }

    /**
     * 实现Comparable接口，使任务可以比较优先级
     * 优先级高的任务排在前面（数字越大优先级越高）
     */
    @Override
    public int compareTo(PriorityTask other) {
        return Integer.compare(other.priority, this.priority);
    }
}
