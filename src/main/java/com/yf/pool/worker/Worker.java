package com.yf.pool.worker;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Worker extends Thread {
    private Boolean isCore;
    private ThreadPool threadPool;
    private Boolean coreDestroy;//如果非核心线程就设置为null
    private Integer aliveTime;
    private Runnable onTimeTask;
    private Runnable loopTask = ()->
    {
        while (true) {
            try {
                Runnable runnable;
                if(isCore) {
                    if(coreDestroy) {
                        runnable = threadPool.getTaskQueue().poll(aliveTime);
                        if(runnable == null){
                            threadPool.getCoreList().remove(this);
                            break;
                        }
                    }else{
                        runnable = threadPool.getTaskQueue().poll(null);
                    }
                }else{
                    runnable = threadPool.getTaskQueue().poll(aliveTime);
                    if(runnable == null){
                        threadPool.getExtraList().remove(this);
                        break;
                    }
                }
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public Worker(ThreadPool threadPool, Boolean isCore, String threadName, Boolean isDaemon, Boolean coreDestroy, Integer aliveTime, Runnable onTimeTask) {
        if(isCore) {
            this.setName(threadName+":core");
        }else{
            this.setName(threadName+":extra");
        }
        this.setDaemon(isDaemon);
        this.threadPool = threadPool;
        this.isCore = isCore;
        this.coreDestroy = coreDestroy;
        this.aliveTime = aliveTime;
        this.onTimeTask = onTimeTask;
    }

    @Override
    public void run() {
        if(onTimeTask !=null){
            onTimeTask.run();
            onTimeTask = null;
        }
        loopTask.run();
    }
}
