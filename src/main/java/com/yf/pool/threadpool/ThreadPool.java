package com.yf.pool.threadpool;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.constant.OfRejectStrategy;
import com.yf.pool.constant.OfWorker;
import com.yf.pool.entity.PoolInfo;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.util.GlobalLock;
import com.yf.pool.worker.Worker;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private Set<Worker> coreList = new HashSet<>();
    private Set<Worker> extraList = new HashSet<>();

    private String name;//线程池名称

    public ThreadPool(
            Integer coreNums, Integer maxNums, String name,
            ThreadFactory threadFactory, TaskQueue taskQueue,
            RejectStrategy rejectStrategy
    ) {
        this.threadFactory = threadFactory;
        threadFactory.setThreadName(name+":"+threadFactory.getThreadName());
        threadFactory.setThreadPool( this);
        this.taskQueue = taskQueue;
        this.rejectStrategy = rejectStrategy;
        rejectStrategy.setThreadPool(this);
        this.coreNums = coreNums;
        this.maxNums = maxNums;
        this.name = name;
        //添加队列和策略的名称
        OfQueue.TASK_QUEUE_MAP.forEach((qName, clazz) -> {
            if(clazz==taskQueue.getClass()){
                this.queueName = qName;
            }
        });
        OfRejectStrategy.REJECT_STRATEGY_MAP.forEach((qName, clazz) -> {
            if(clazz==rejectStrategy.getClass()){
                this.rejectStrategyName = qName;
            }
        });

    }

    /**
     * 执行任务
     */
    public void execute(Runnable task){
        try{
            lock.lock();
            if (coreList.size() < coreNums) {
                threadFactory.createWorker(true, task).start();
                return;
            }
            else if (coreList.size() + extraList.size() < maxNums) {
                threadFactory.createWorker(false, task).start();
                return;
            }
        }finally {
            lock.unlock();
        }
        if(taskQueue.getCapacity() == null){//说明无界
            taskQueue.addTask(task);
            return;
        }
        Boolean success = taskQueue.addTask(task);
        if(!success) {
            rejectStrategy.reject(task);
        }
    }

    /**
     *
     * 提交任务，有返回值
     * @return future容器
     */
    public Future submit(Callable<Object> task){
        FutureTask futureTask = new FutureTask(task);
        try{
            lock.lock();
            if (coreList.size() < coreNums) {
                threadFactory.createWorker(true, futureTask).start();
                return futureTask;
            }
            else if (coreList.size() + extraList.size() < maxNums) {
                threadFactory.createWorker(false, futureTask).start();
                return futureTask;
            }
        }finally {
            lock.unlock();
        }
        if(taskQueue.getCapacity() == null){//说明无界
            taskQueue.addTask(futureTask);
            return futureTask;
        }
        Boolean success = taskQueue.addTask(futureTask);
        if(success) {
            return futureTask;
        }
         return rejectStrategy.reject(futureTask);
    }

    /**
     * 获取线程池中线程的信息
     * @return ：String代表线程类别，Thread.State代表线程状态，Integer代表对应类别的状态的数量
     */
    public Map<String,Map<Thread.State,Integer>> getThreadsInfo(){
        Map<String,Map<Thread.State,Integer>> result = new HashMap<>();
        Map<Thread.State,Integer> coreMap = new HashMap<>();
        Map<Thread.State,Integer> extraMap = new HashMap<>();
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
        result.put(OfWorker.CORE,coreMap);
        result.put(OfWorker.EXTRA,extraMap);
        return result;
    }

    /**获取线程池信息
     * @return
     */
    public PoolInfo getThreadPoolInfo() {
        PoolInfo info = new PoolInfo();
        GlobalLock.READ_LOCK.lock();
        try {
            info.setPoolName( name);
            info.setAliveTime( threadFactory.getAliveTime());
            info.setThreadName( threadFactory.getThreadName());
            info.setCoreDestroy( threadFactory.getCoreDestroy());
            info.setIsDaemon( threadFactory.getIsDaemon());
            info.setMaxNums( maxNums);
            info.setCoreNums( coreNums);
            info.setQueueCapacity( taskQueue.getCapacity());
            info.setQueueName( queueName);
            info.setRejectStrategyName( rejectStrategyName);
        } catch (Exception e) {
            lock.unlock();
            e.printStackTrace();
        }
        return info;
     }

}
