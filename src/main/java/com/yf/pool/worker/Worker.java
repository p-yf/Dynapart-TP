package com.yf.pool.worker;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yyf
 * @description
 */
@Slf4j
@Getter
@Setter
public class Worker extends Thread {
    volatile private Boolean flag = true;
    volatile private Boolean isCore;
    private ThreadPool threadPool;
    volatile private Boolean coreDestroy;//如果非核心线程就设置为null
    volatile private Integer aliveTime;//核心线程如果允许销毁则为这个数的两倍
    private Runnable onTimeTask;//是指直接提交运行的任务
    private Runnable loopTask = ()->//这里是循环从队列拿到任务
    {
        while (flag) {
            try {
                Runnable runnable;
                if(isCore) {
                    if(coreDestroy) {//核心线程并且允许销毁
                        runnable = threadPool.getTaskQueue().getTask(aliveTime*2);
                        if(runnable == null){
                            threadPool.getCoreList().remove(this);
                            log.info("核心线程"+getName()+"销毁");
                            break;
                        }
                    }else{//核心线程并且不允许销毁
                        runnable = threadPool.getTaskQueue().getTask(null);
                    }
                }else{//非核心线程
                    runnable = threadPool.getTaskQueue().getTask(aliveTime);
                    if(runnable == null){
                        threadPool.getExtraList().remove(this);
                        log.info("非核心线程"+getName()+"销毁");
                        break;
                    }
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
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
            try {
                onTimeTask.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            onTimeTask = null;
        }
        loopTask.run();
    }
}
