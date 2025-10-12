package com.yf.core.workerfactory;

import com.yf.common.constant.Constant;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.worker.Worker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yyf
 * @description
 */
@Slf4j
@Data
public class WorkerFactory {
    private ThreadPool threadPool;
    private String threadName;// 线程名称
    private boolean useDaemonThread;//是否守护线程
    private boolean coreDestroy;//是否销毁核心线程
    private Integer aliveTime;//空闲存活时间 :null代表不销毁
    private boolean useVirtualThread;//是否虚拟线程

    public WorkerFactory(String threadName, Boolean isDaemon, Boolean coreDestroy, Integer aliveTime){
        this.threadName = threadName;
        this.useDaemonThread = isDaemon;
        this.coreDestroy = coreDestroy;
        this.aliveTime = aliveTime;
    }
    public WorkerFactory(String threadName, Boolean isDaemon, Boolean coreDestroy, Integer aliveTime,boolean useVirtualThread) {
        this.threadName = threadName;
        this.useDaemonThread = isDaemon;
        this.coreDestroy = coreDestroy;
        this.aliveTime = aliveTime;
        this.useVirtualThread = useVirtualThread;
    }
    public Worker createWorker(Boolean isCore, Runnable task){
        Worker worker = new Worker(threadPool,isCore,coreDestroy,aliveTime,task);

        if(useVirtualThread){//创建虚拟线程
            Thread thread = Thread.ofVirtual().name(Constant.VIRTUAL +threadName).unstarted( worker);
            worker.setThread(thread);
        } else{//创建平台线程
            Thread thread = new Thread(worker, Constant.PLATFORM+threadName);
            thread.setDaemon(useDaemonThread);
            worker.setThread(thread);
        }
        return worker;
    }

}
