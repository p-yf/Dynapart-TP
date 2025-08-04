# DynaGuardAutoPool

<img src="src/main/resources/static/logo/logo.png" alt="Logo" width="200" height="200">  <!-- 项目Logo -->

## 🚀 项目介绍

DynaGuardAutoPool 是一个高性能、可动态调整的轻量级线程池框架，专为需要精细控制并发任务执行的Java应用程序设计。该框架提供了丰富的线程池管理功能，包括参数动态调整、实时监控和灵活的任务调度策略。

### 核心功能
- **动态参数调整**：无需重启应用，实时调整核心线程数、最大线程数，甚至是任务队列等参数
- **多队列支持**：可切换不同类型的任务队列（如阻塞队列、优先级队列）
- **灵活的拒绝策略**：支持多种任务拒绝策略，并可动态切换
- **实时监控**：通过REST API和WebSocket实时监控线程池状态
- **Spring Boot集成**：与Spring Boot框架无缝集成，支持自动配置
- **轻松扩展**：只需要实现接口就能轻松自定义任务队列、拒绝策略

## 💡 实现亮点

### 1. 动态参数调整机制
框架允许在运行时调整线程池的核心参数，包括核心线程数、最大线程数、线程空闲时间等。这些调整会立即生效，无需重启应用。

### 2. 实时监控系统
通过WebSocket实现了线程池状态的实时推送，客户端可以实时获取线程池的工作状态、任务队列长度等信息。同时提供了REST API用于查询和调整线程池参数。

### 3. 可插拔的组件设计
- **任务队列**：支持多种队列实现，可根据业务需求动态切换
- **拒绝策略**：提供多种拒绝策略（如CallerRuns、DiscardOldest等），并支持自定义策略

### 4. 线程生命周期管理
精细控制线程的创建和销毁，支持核心线程的动态调整和非核心线程的自动回收。

## 📚 使用方法

### 1. Spring Boot环境集成(test_springboot_integration包就是用来测试springboot集成的)

#### 配置文件
在`application.properties`或`application.yml`中添加以下配置：

```properties
fy:
  thread-pool:
    enabled: true  #是否开启线程池自动装配
    coreNums: 5    #线程池核心线程数
    maxNums: 10    #线程池最大线程数
    poolName: yf-thread-pool   #线程池名称
    threadName: yf-thread      #线程名称
    isDaemon: true      #是否是守护线程
    coreDestroy: false  #线程池是否销毁
    aliveTime: 5        #线程存活时间（单位：s）
    queueName: linked   #队列名称
    queueCapacity:      #队列容量（不写代表null，为无界）
    rejectStrategyName: callerRuns   #拒绝策略名称
    monitor:
      enabled: true       #是否开启监控
      fixedDelay: 1000    #后台像前端推送线程状态信息的间隔时间（单位：ms）
      qReplaceable: true  #是否可更换队列     默认是开启的（不写这个key默认开启，写了key后只有value为true才是开启，以下同理）
      rsReplaceable: true #是否可更换拒绝策略
```

#### 使用线程池

注入`ThreadPool`实例并使用：

```java
@Autowired
private ThreadPool threadPool;

// 执行任务
threadPool.execute(() -> {
    // 任务逻辑
});
//如果是使用优先级队列，那么应传入PriorityTask对象能手动指定优先级（数字越大优先级越高），示例：
//Runnable r = () -> {};        //任务，返回值 ，优先级
//PriorityTask pt = new PriorityTask(r, null,10);
//threadPool.executeThreadFirst(pt);


// 提交有返回值的任务
Future<?> future = threadPool.submit(() -> {
    // 任务逻辑
    return result;
});
//如果是使用优先级队列，那么应传入PriorityTask对象能手动指定优先级（数字越大优先级越高），示例：
//Callable c = () -> {};        //任务，返回值 ，优先级
//PriorityTask pt = new PriorityTask(c,10);
//threadPool.executeThreadFirst(pt);
```

### 2. 监控线程池

#### REST API
- 获取线程池信息：`GET /monitor/pool`
- 获取队列任务数量：`GET /monitor/tasks`
- 调整线程参数：`PUT /monitor/worker`
- 切换队列：`PUT /monitor/queue`
- 切换拒绝策略：`PUT /monitor/rejectStrategy`

#### WebSocket实时监控
连接`/monitor/threads`端点，实时接收线程池状态更新。



这种方式会优先使用配置文件中定义的`server.port`，如果没有定义则默认使用8080端口。

### 3. 非Spring Boot环境使用

```java
// 创建线程工厂
ThreadFactory threadFactory = new ThreadFactory("worker", false, false, 6);
                                            线程名称，是否守护，核心是否摧毁，空闲时间（单位：s）
// 创建任务队列
TaskQueue taskQueue = new LinkedBlockingQueue(100);
                                            队列容量，如果为null，则代表无界
// 创建拒绝策略
RejectStrategy rejectStrategy = new CallerRunsStrategy();

// 创建线程池                           
ThreadPool threadPool = new ThreadPool(5, 20, //核心数量，最大数量
                                      "DynaGuardPool",//线程池名称
                                       threadFactory,//线程工厂
                                        taskQueue,//任务队列
                                         rejectStrategy);//拒绝策略

// 使用线程池
threadPool.execute(() -> {
    // 任务逻辑
});
threadPool.submit(() -> {
    // 任务逻辑
});
```

## 📝 注意事项
- 确保在生产环境中合理配置线程池参数，避免资源耗尽
- 动态调整参数时，注意核心线程数不能超过最大线程数
- 自定义队列时，需确保线程安全

## 🔧 开发者扩展说明

### 自定义任务队列
要实现自定义任务队列，只需继承`TaskQueue`抽象类并实现其抽象方法：

```java
// @TaskQueueBean("custom")//springboot环境加上，yml文件配置好队列名称，可实现自动装配
public class CustomQueue extends TaskQueue {//需要保证线程安全，读写锁以及条件变量抽象父类已经提供
    private Queue<Runnable> q;

    public CustomQueue(Integer capacity) {
    //如果是springboot环境就不要加上这个构造方法，因为容器中没有capacity的bean
        setCapacity(capacity);
    }   

    @Override
    public Boolean addTask(Runnable task) {
        //添加任务逻辑
    }

    @Override
    public Runnable poll(Integer waitTime) throws InterruptedException {
        // 获取任务逻辑
    }

    @Override
    public Boolean removeTask() {
        // 移除任务逻辑
    }

    @Override
    public int getExactTaskNums() {
        // 获取此时精确任务数量逻辑（需要锁来保证）
    }

    @Override
    public int getTaskNums() {
        // 获取此时任务数量逻辑（不需要锁来保证）
    }
}
```

然后，在`OfQueue`常量类中注册你的自定义队列（如果是本项目的springboot环境下的使用者则无需关注。如果不用springboot环境，那么需要在初始化线程池之前添加你扩展的队列的名称和类到map中。如果是本项目开发者则需要添加好名称后再在静态代码块中添加上队列名称与类）：

```java
public class OfQueue {
    public final static String CUSTOM = "custom";
    // 其他队列类型...
    
    static {
        TASK_QUEUE_MAP.put(LINKED, LinkedBlockingQueue.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQueue.class);
        TASK_QUEUE_MAP.put(CUSTOM, CustomQueue.class);  // 注册自定义队列
    }
}
```

### 自定义拒绝策略
要实现自定义拒绝策略，只需实现`RejectStrategy`接口：

```java
//使用与队列同理
public class CustomRejectStrategy implements RejectStrategy {
    private ThreadPool threadPool;

    @Override
    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public void reject(Runnable task) {
        // 自定义拒绝逻辑
        System.err.println("Custom rejection strategy: Task rejected - " + task);
        // 例如，可以记录日志、尝试重新提交或执行其他操作
    }
}
```

然后，在`OfRejectStrategy`常量类中注册你的自定义拒绝策略：

```java
public class OfRejectStrategy {
    public final static String CUSTOM = "custom";
    // 其他拒绝策略...
    
    static {
        REJECT_STRATEGY_MAP.put(CALLER_RUNS, CallerRunsStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD_OLDEST, DiscardOldestStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD, DiscardStrategy.class);
        REJECT_STRATEGY_MAP.put(CUSTOM, CustomRejectStrategy.class);  // 注册自定义拒绝策略
    }
}
```

## 📚 技术文档

### 核心技术实现

#### 1. 线程池核心实现
- **动态参数调整**：通过`ThreadPool`类中的方法实现核心线程数、最大线程数等参数的动态调整
- **线程生命周期管理**：通过`Worker`类中的循环任务和超时机制实现线程的创建和自动回收
- **任务调度**：使用`TaskQueue`接口定义的队列实现任务的存储和调度

#### 2. 并发控制机制
- 使用`ReentrantLock`和`ReadWriteLock`保证线程安全
- 使用`Condition`实现线程间的通信和等待唤醒机制
- 采用精细的锁粒度，避免全局锁带来的性能瓶颈

#### 3. 组件设计模式
- **策略模式**：用于实现不同的拒绝策略和任务队列
- **工厂模式**：通过`ThreadFactory`创建线程
- **观察者模式**：通过WebSocket实现线程池状态的实时推送

#### 4. Spring Boot集成
- 使用`@ConfigurationProperties`和`@AutoConfiguration`实现自动配置
- 通过`@ConditionalOnProperty`和`@Conditional`实现条件装配
- 提供`ThreadPoolProperties`类让用户可以通过配置文件自定义线程池参数

#### 5. 监控系统
- **REST API**：提供HTTP接口用于查询和调整线程池参数
- **WebSocket**：通过`ThreadPoolWebSocketHandler`实现线程池状态的实时推送
- **前端监控界面**：使用HTML、Tailwind CSS和JavaScript实现可视化监控界面（豆包生成的哦）

#### 6.未来集成命令行......
