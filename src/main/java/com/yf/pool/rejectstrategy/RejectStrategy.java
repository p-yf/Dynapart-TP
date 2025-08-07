package com.yf.pool.rejectstrategy;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;



/**
 * @author yyf
 * @description
 */
@Getter
@Setter
public abstract class  RejectStrategy {

    private ThreadPool threadPool;
    public abstract void reject (Runnable task);//处理普通任务的

}
