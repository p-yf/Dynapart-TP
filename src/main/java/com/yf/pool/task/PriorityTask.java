package com.yf.pool.task;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

@Setter
@Getter
public class PriorityTask<V> extends FutureTask<V> implements Comparable<PriorityTask<V>> {
    private int priority = 0;

    public PriorityTask(Callable<V> callable, int priority) {
        super(callable);
        this.priority = priority;
    }

    public PriorityTask(Runnable runnable, int priority) {
        // 无返回值时，result 传 null，且泛型固定为 Void
        super(runnable, null);
        this.priority = priority;
    }

    @Override
    public int compareTo(PriorityTask<V> other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(other.getPriority(), this.getPriority());
    }
}
