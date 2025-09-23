package com.yf.core.rejectstrategy;

import com.yf.core.threadpool.ThreadPool;



/**
 * @author yyf
 * @description
 */
public abstract class  RejectStrategy {

    public abstract void reject (ThreadPool threadPool,Runnable task);//处理普通任务的

}
