package com.yf.core.worker;

import com.yf.common.constant.Logo;
import com.yf.core.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author yyf
 * @description
 */
@Slf4j
@Getter
@Setter
public class Worker extends Thread {
    private Lock lock = new ReentrantLock();
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
                        runnable = threadPool.getPartition().poll(aliveTime*2);
                        if(runnable == null){
                            threadPool.getCoreList().remove(this);
                            threadPool.getCoreWorkerCount().getAndDecrement();
                            log.info("核心线程"+getName()+"销毁");
                            break;
                        }
                    }else{//核心线程并且不允许销毁
                        runnable = threadPool.getPartition().poll(null);
                    }
                }else{//非核心线程
                    runnable = threadPool.getPartition().poll(aliveTime);
                    if(runnable == null){
                        threadPool.getExtraList().remove(this);
                        threadPool.getExtraWorkerCount().getAndDecrement();
                        log.info("非核心线程"+getName()+"销毁");
                        break;
                    }
                }
                try {
                    lock.lock();//获取锁才能执行任务，防止任务中有可以被打断的等待逻辑在等待中被打断
                    runnable.run();
                } catch (Exception e) {
                    lock.unlock();
                    log.error(Logo.LOG_LOGO+"Worker线程执行任务中发生异常", e);
                }
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
                log.error(Logo.LOG_LOGO+"Worker线程执行任务之外发生异常", e);
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
        try {
            if (onTimeTask != null) {
                try {
                    onTimeTask.run();
                } catch (Throwable t) {
                    log.error("onTimeTask执行异常", t);
                } finally {
                    onTimeTask = null; // 无论是否异常，都清空初始任务
                }
            }
            loopTask.run();
        } catch (Throwable t) {
            log.error("Worker线程异常终止", t);
        }
    }

    public void lock(){
        lock.lock();
    }
    public void unlock(){
        lock.unlock();
    }
}
