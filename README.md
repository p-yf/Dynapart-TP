# DynaPart-TP

<img src="src/main/resources/static/logo/logo.png" alt="Logo" width="200" height="200">  <!-- 项目Logo -->

## 🚀 项目介绍

DynaPart-TP 是一个高性能、可动态调整的轻量级线程池框架，专为需要精细控制并发任务执行的Java应用程序设计。该框架提供了丰富的线程池管理功能，包括参数动态调整、实时监控和灵活的任务调度策略。

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

### 3. 可视化监控中心
提供了Web界面的监控中心，可直观查看线程池状态、配置参数，并支持在线修改配置。访问`http://localhost:8080/configuration.html`即可进入监控中心。

### 4. 可插拔的组件设计
- **任务队列**：支持多种队列实现，可根据业务需求动态切换
- **拒绝策略**：提供多种拒绝策略（如CallerRuns、DiscardOldest等），并支持自定义策略

### 5. 线程生命周期管理
精细控制线程的创建和销毁，支持核心线程的动态调整和非核心线程的自动回收。

### 6. 分区化队列模型（Partition）
分区化是框架的核心特性之一，它将队列抽象为一种分区表现形式。任何队列只要实现了`Partition`接口，就可以自由选择成为分区队列或者单个队列。

- **灵活的分区策略**：支持轮询、随机、哈希、填谷等多种任务入队策略
- **高效的任务出队**：提供轮询、随机、削峰、线程绑定等出队策略
- **细粒度控制**：可根据业务需求动态调整分区数量和容量
- **高性能设计**：通过多分区并行处理提高吞吐量，减少锁竞争

### 7. 命令行交互功能
提供命令行接口，支持在线查询线程池状态、修改配置参数等操作，如`yf info pool`、`yf change worker`等。

### 8. Redis集成支持
框架支持与Redis集成，可用于分布式环境下的线程池状态共享和任务调度。

## 🚀 性能对比

通过测试对比，DynaPart-TP线程池与JDK线程池相比具有明

### 性能优势
1. **更高吞吐量**：在相同配置下，DynaPart-TP处理任务的速度比JDK线程池快约5%-10%
2. **更好的资源利用**：通过分区化设计，减少线程等待时间，提高CPU利用率
3. **更稳定的性能**：在高并发场景下，DynaPart-TP的性能波动更小

## 📚 使用方法

### 1. Spring Boot环境集成(test_springboot_integration包就是用来测试springboot集成的)

#### 1.1配置文件
在`application.yml`中添加以下配置：

```yaml
server:
  port: 8080

#线程池配置
yf:
  thread-pool:
    pool:
      enabled: true
      coreNums: 5    #线程池核心线程数
      maxNums: 10    #线程池最大线程数
      poolName: yf-thread-pool   #线程池名称
      threadName: yf-thread      #线程名称
      isDaemon: true      #是否是守护线程
      coreDestroy: false  #核心线程是否销毁
      aliveTime: 5000        #线程存活时间（单位：ms）
      rejectStrategyName: callerRuns   #拒绝策略名称
    queue:     #(由于queue比较重要，所以与pool和monitor一个层级)
      partitioning: true  #是否分区化(如果是false，只需要读取capacity和queueName)
      partitionNum: 10      #分区数量
      capacity: 500         #队列容量（不写代表null，为无界）
      queueName: linked_plus     #队列名称
      offerStrategy:       #入队策略
      pollStrategy:        #出队策略
      removeStrategy:      #移除策略
    monitor:
      enabled: true       #是否开启监控
      fixedDelay: 5000    #后台像前端推送线程状态信息的间隔时间(单位：ms)
      qReplaceable: false  #是否可更换队列     默认是开启的（不写这个key默认开启，写了key后只有value为true才是开启，以下同理）
      rsReplaceable: false #是否可更换拒绝策略
    service-registry:      #是否开启服务注册
        enabled: true

```

#### 1.2使用线程池

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

### 1.3监控线程池

#### REST API
- 获取线程池信息：`GET /monitor/pool`
- 获取队列任务数量：`GET /monitor/tasks`
- 调整线程参数：`PUT /monitor/worker`
- 切换队列：`PUT /monitor/queue`
- 切换拒绝策略：`PUT /monitor/rejectStrategy`

#### WebSocket实时监控
连接`/monitor/threads`端点，实时接收线程池状态更新。



这种方式会优先使用配置文件中定义的`server.port`，如果没有定义则默认使用8080端口。

### 2. 非Spring Boot环境使用

```java
// 创建线程工厂
ThreadFactory threadFactory = new ThreadFactory("worker", false, false, 6000);
                                            // 线程名称，是否守护线程，核心线程是否销毁，空闲时间（单位：ms）


singleQueue.setCapacity(100); // 设置队列容量，如果不设置则为无界队列

// 或创建分区化队列
PartiFlow<Runnable> partitionedQueue = new PartiFlow<>(
    10, // 分区数量
    1000, // 总容量
    “linked_plus”, // 队列名称
    OfferStrategy.ROUND_ROBIN, // 入队策略
    PollStrategy.ROUND_ROBIN, // 出队策略
    RemoveStrategy.ROUND_ROBIN // 移除策略
);

// 创建拒绝策略
RejectStrategy rejectStrategy = new CallerRunsStrategy();

// 创建线程池                            
ThreadPool threadPool = new ThreadPool(
    5, 20, // 核心线程数，最大线程数
    "DynaPartPool", // 线程池名称
    threadFactory, // 线程工厂
    singleQueue, // 任务队列（或使用partitionedQueue）
    rejectStrategy // 拒绝策略
);

// 使用线程池
threadPool.execute(() -> {
    // 任务逻辑
});

Future<?> future = threadPool.submit(() -> {
    // 任务逻辑
    return "Result";
});
```
### 2.1 命令行

#### 主要命令
- yf info pool //打印线程池信息
- yf info worker  //打印线程信息
- yf info taskNum  //打印队列任务数量
- yf change worker -coreNums 2 -maxNums 5 -coreDestroy true......(如果有参数没写就直接赋值为null)  //改变线程参数
- yf change queue linked(队列名称举例)  //改变队列
- yf change rejectstrategy callerRuns(拒绝策略名称举例)   //改变拒绝策略


## 🔧 开发者扩展说明

### 自定义任务队列
要实现自定义任务队列，只需继承`Partition`抽象类并实现其抽象方法：

```java
// Spring Boot环境下使用该注解并指定队列名称
@PartitionBean("custom")
public class CustomQueue<T> extends Partition<T> {
    // 需要保证线程安全

    public CustomQueue() {
    }

    @Override
    public Boolean offer(T task) {

    }

    // 可选：实现warning方法，在添加任务后执行警告逻辑
    public void warning() {

    }

    @Override
    public T getEle(Integer waitTime) throws InterruptedException {

    }

    @Override
    public Boolean removeEle() {

    }

    @Override
    public int getEleNums() {
    }

    @Override
    public void lockGlobally() {
    }

    @Override
    public void unlockGlobally() {
    }

    @Override
    public Integer getCapacity() {
    }

    @Override
    public void setCapacity(Integer capacity) {
    }
}
```

然后，在`OfQueue`常量类中注册你的自定义队列（本项目开发者需要，使用者需要只需要将map.put(:"队列名称",队列类型)就行）：

```java
public class OfQueue {
    public final static String CUSTOM = "custom";
    // 其他队列类型...
    
    static {
        // 已有的队列注册
        TASK_QUEUE_MAP.put(LINKED_MINI, LinkedBlockingQMini.class);
        TASK_QUEUE_MAP.put(LINKED_PLUS, LinkedBlockingQPlus.class);
        TASK_QUEUE_MAP.put(PRIORITY, PriorityBlockingQueue.class);
        
        // 注册自定义队列
        TASK_QUEUE_MAP.put(CUSTOM, CustomQueue.class);
    }
}
```

### 自定义拒绝策略
要实现自定义拒绝策略，只需继承`RejectStrategy`抽象类：

```java
// Spring Boot环境下使用该注解并指定策略名称
@RejectStrategyBean("custom")
public class CustomRejectStrategy extends RejectStrategy {
    @Override
    public void reject(Runnable task) {

    }
}
```

然后，在`OfRejectStrategy`常量类中注册你的自定义拒绝策略（注册逻辑与上述队列一致）：

```java
public class OfRejectStrategy {
    public final static String CUSTOM = "custom";
    // 其他拒绝策略...
    
    static {
        // 已有的策略注册
        REJECT_STRATEGY_MAP.put(CALLER_RUNS, CallerRunsStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD_OLDEST, DiscardOldestStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD, DiscardStrategy.class);
        
        // 注册自定义策略
        REJECT_STRATEGY_MAP.put(CUSTOM, CustomRejectStrategy.class);
    }
}
```

## 📚 技术文档

### 核心技术实现

#### 1. 线程池核心实现
- **动态参数调整**：通过`ThreadPool`类中的方法实现核心线程数、最大线程数等参数的动态调整
- **线程生命周期管理**：通过`Worker`类中的循环任务和超时机制实现线程的创建和自动回收
- **任务调度**：使用`Partition`接口定义的队列实现任务的存储和调度

#### 2. 并发控制机制
- 使用`ReentrantLock`和`ReadWriteLock`保证线程安全
- 使用`Condition`实现线程间的通信和等待唤醒机制
- 采用精细的锁粒度，避免全局锁带来的性能瓶颈,例如线程池任务提交利用cas以及组件内部锁、队列使用头尾锁以及头锁尾cas等等

#### 3. 组件设计模式
- **策略模式**：用于实现不同的拒绝策略和任务队列
- **工厂模式**：通过`ThreadFactory`创建线程
- **模板方法**：在Partition中定义添加任务后报警的模板方法，具体的逻辑子类实现
- **观察者模式**：通过WebSocket实现线程池状态的实时推送

#### 4. Spring Boot集成
- 使用`@ConfigurationProperties`和`@AutoConfiguration`实现自动配置
- 通过`@ConditionalOnProperty`和`@Conditional`实现条件装配
- 提供`ThreadPoolProperties`类让用户可以通过配置文件自定义线程池参数

#### 5. 监控系统
- **REST API**：提供HTTP接口用于查询和调整线程池参数
- **WebSocket**：通过`ThreadPoolWebSocketHandler`实现线程池状态的实时推送
- **前端监控界面**：使用HTML、Tailwind CSS和JavaScript实现可视化监控界面（豆包生成的哦）

#### 6. 分区化概念
- 将队列都抽象为`Partition`，利用抽象基类以及装饰器模式，可以轻松将队列分区化，通过`PartiFlow`类实现多队列并行处理
- 支持多种分区策略：轮询、随机、Hash、填谷等入队策略；轮询、随机、削峰等出队策略
- 极大降低锁粒度，提升系统并发性能和稳定性



