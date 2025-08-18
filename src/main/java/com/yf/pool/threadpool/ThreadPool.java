package com.yf.pool.threadpool;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.constant.OfRejectStrategy;
import com.yf.pool.constant.OfWorker;
import com.yf.pool.entity.PoolInfo;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.worker.Worker;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author yyf
 * @description
 */
@Getter
@Setter
public class ThreadPool {
    private Boolean isSpringBootEnvironment;
    private Lock lock = new ReentrantLock();
    private ThreadFactory threadFactory;
    private TaskQueue taskQueue;
    private RejectStrategy rejectStrategy;
    private Integer coreNums;//核心线程数
    private Integer maxNums;//最大线程数
    private String queueName = null;//非springboot环境直接用类，反之用名字,拒绝策略同理
    private String rejectStrategyName = null;
    private Set<Worker> coreList = ConcurrentHashMap.newKeySet();
    private Set<Worker> extraList = ConcurrentHashMap.newKeySet();
    private final AtomicInteger coreWorkerCount = new AtomicInteger(0); // 核心线程存活数
    private final AtomicInteger extraWorkerCount = new AtomicInteger(0); // 额外线程存活数

    private String name;//线程池名称

    public ThreadPool(
            Integer coreNums, Integer maxNums, String name,
            ThreadFactory threadFactory, TaskQueue taskQueue,
            RejectStrategy rejectStrategy
    ) {
        this.threadFactory = threadFactory;
        threadFactory.setThreadName(name + ":" + threadFactory.getThreadName());
        threadFactory.setThreadPool(this);
        this.taskQueue = taskQueue;
        this.rejectStrategy = rejectStrategy;
        rejectStrategy.setThreadPool(this);
        this.coreNums = coreNums;
        this.maxNums = maxNums;
        this.name = name;
        //添加队列和策略的名称
        OfQueue.TASK_QUEUE_MAP.forEach((qName, clazz) -> {
            if (clazz == taskQueue.getClass()) {
                this.queueName = qName;
            }
        });
        OfRejectStrategy.REJECT_STRATEGY_MAP.forEach((qName, clazz) -> {
            if (clazz == rejectStrategy.getClass()) {
                this.rejectStrategyName = qName;
            }
        });

    }

    public void execute(Runnable task) {
        if (coreList.size() < coreNums) {
            if (addWorker(task,true)) {
                return;
            }
        }
        if (taskQueue.offer(task)) {
            return;
        }
        if (addWorker(task,false)) {
            return;
        }
        rejectStrategy.reject(task);
    }


    public Future submit(Callable<Object> callable) {
        FutureTask task = new FutureTask(callable);
        if (coreList.size() < coreNums) {
            if (addWorker(task,true)) {
                return  task;
            }
        }
        if (taskQueue.offer(task)) {
            return  task;
        }
        if (addWorker(task,false)) {
            return  task;
        }
        rejectStrategy.reject(task);
        task.cancel(true);
        return task;
    }


    private boolean addWorker(Runnable task, boolean isCore) {
        // 循环CAS重试：直到成功或确定无法创建线程
        while (true) {
            if (isCore) {
                int current = coreWorkerCount.get();
                if (current >= coreNums) {
                    return false; // 核心线程达到上限，退出
                }
                // CAS尝试更新
                if (coreWorkerCount.compareAndSet(current, current + 1)) {
                    break; // CAS成功,即将退出循环创建Worker
                }// CAS失败，继续循环重试
            } else {
                // 额外线程逻辑
                int current = extraWorkerCount.get();
                int extraMax = maxNums - coreNums;
                if (current >= extraMax) {
                    return false; // 额外线程达到上限，退出
                }
                if (extraWorkerCount.compareAndSet(current, current + 1)) {
                    break; // CAS成功，退出循环
                }// CAS失败，继续循环重试
            }
            Thread.yield();
        }
        Worker worker = null;
        try {
            worker = threadFactory.createWorker(isCore, task);
            if (isCore) {
                coreList.add(worker);
            } else {
                extraList.add(worker);
            }
            worker.start();
            return true;
        } catch (Throwable e) {//异常回滚
            if (isCore) {
                coreWorkerCount.getAndDecrement();
            } else {
                extraWorkerCount.getAndDecrement();
            }
            if (worker != null) {
                if (isCore) {
                    coreList.remove(worker);
                } else {
                    extraList.remove(worker);
                }
            }
            return false;
        }
    }

//    =======================================================
//    以下是对于线程池相关参数的读写操作，用来提供监控信息和修改参数，不涉及到线程池运行过程的自动调控，所以读取信息全部无锁

    /**
     * 获取线程池中线程的信息
     *
     * @return ：String代表线程类别，Thread.State代表线程状态，Integer代表对应类别的状态的数量
     */
    public Map<String, Map<Thread.State, Integer>> getThreadsInfo() {
        Map<String, Map<Thread.State, Integer>> result = new HashMap<>();
        Map<Thread.State, Integer> coreMap = new HashMap<>();
        Map<Thread.State, Integer> extraMap = new HashMap<>();
        for (Worker worker : coreList) {
            Thread.State state = worker.getState();
            if (coreMap.containsKey(state)) {
                coreMap.put(state, coreMap.get(state) + 1);
            } else {
                coreMap.put(state, 1);
            }
        }
        for (Worker worker : extraList) {
            Thread.State state = worker.getState();
            if (extraMap.containsKey(state)) {
                extraMap.put(state, extraMap.get(state) + 1);
            } else {
                extraMap.put(state, 1);
            }
        }
        result.put(OfWorker.CORE, coreMap);
        result.put(OfWorker.EXTRA, extraMap);
        return result;
    }

    /**
     * 获取线程池信息
     *
     * @return
     */
    public PoolInfo getThreadPoolInfo() {
        PoolInfo info = new PoolInfo();
        info.setPoolName(name);
        info.setAliveTime(threadFactory.getAliveTime());
        info.setThreadName(threadFactory.getThreadName());
        info.setCoreDestroy(threadFactory.getCoreDestroy());
        info.setIsDaemon(threadFactory.getIsDaemon());
        info.setMaxNums(maxNums);
        info.setCoreNums(coreNums);
        info.setQueueCapacity(taskQueue.getCapacity());
        info.setQueueName(queueName);
        info.setRejectStrategyName(rejectStrategyName);
        return info;
    }

    /**
     * 获取队列中任务数量
     * @return
     */
    public int getTaskNums() {
        return taskQueue.getTaskNums();
    }


    /**
     * 改变worker相关参数，直接赋值就好，会动态平衡的
     */
    public Boolean changeWorkerParams(Integer coreNums, Integer maxNums, Boolean coreDestroy, Integer aliveTime, Boolean isDaemon) {
        if (maxNums!=null&&coreNums!=null&&maxNums < coreNums) {
            return false;
        }
        int oldCoreNums = this.coreNums;
        int oldMaxNums = this.maxNums;
        if(coreNums!=null){
            if(maxNums==null){
                if(coreNums > oldMaxNums){
                    return false;
                }
            }
        }else{
            if(maxNums!=null&&maxNums < oldCoreNums){
                return false;
            }
        }
        if(maxNums!=null) {
            this.maxNums = maxNums;//无论如何非核心线程都能直接改变
        }
        if(coreNums!=null) {
            this.coreNums = coreNums;
        }
        if(coreNums==null){
            if(maxNums!=null) {
                destroyWorkers(0, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }else{
            if(maxNums==null) {
                destroyWorkers(oldCoreNums - coreNums, 0);
            }else{
                destroyWorkers(oldCoreNums - coreNums, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }
        if (aliveTime != null) {
            this.threadFactory.setAliveTime(aliveTime);
        }
        if (coreDestroy != null) {
            this.threadFactory.setCoreDestroy(coreDestroy);
        }
        if (isDaemon != null && isDaemon != this.threadFactory.getIsDaemon()) {
            this.threadFactory.setIsDaemon(isDaemon);
            for (Worker worker : getCoreList()) {
                worker.setDaemon(isDaemon);
            }
            for (Worker worker : getExtraList()) {
                worker.setDaemon(isDaemon);
            }
        }
        return true;
    }

    //销毁线程
    public void destroyWorkers(int coreNums, int extraNums) {//销毁的数量
        if (coreNums > 0) {
            int i = 0;
            for (Worker worker : getCoreList()) {
                worker.setFlag(false);
                worker.interrupt();
                i++;
                if (i == coreNums) {
                    break;
                }
            }
        }
        if (extraNums > 0) {
            int j = 0;
            for (Worker worker : getExtraList()) {
                worker.setFlag(false);
                worker.interrupt();
                j++;
                if (j == extraNums) {
                    break;
                }
            }
        }
    }

    /**
     * 改变队列
     */
    public Boolean changeQueue(TaskQueue q,String qName){
        if(q == null|| qName == null){
            return false;
        }
        TaskQueue oldQ = taskQueue;
        try {
            oldQ.lockGlobally();
            while(oldQ.getTaskNums() > 0){
                Runnable task = oldQ.getTask(null);//虽然设置为null，代表无限期等待，但是条件为线程池中至少有一个任务，所以不会阻塞
                q.addTask(task);
            }
            this.queueName = qName;
            this.taskQueue = q;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            oldQ.unlockGlobally();
        }
        return true;
    }

    /**
     * 改变拒绝策略,默认与拒绝策略无共享变量需要争抢，所以线程安全，不需要加锁
     */
    public Boolean changeRejectStrategy(RejectStrategy rejectStrategy,String rejectStrategyName){
        if(rejectStrategy == null|| rejectStrategyName == null){
            return false;
        }
        this.rejectStrategyName = rejectStrategyName;
        this.rejectStrategy = rejectStrategy;
        return true;
    }




}
