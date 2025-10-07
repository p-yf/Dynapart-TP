package com.yf.common.command;

import com.yf.common.entity.PoolInfo;
import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.partition.Partition;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.workerfactory.WorkerFactory;

import java.io.Console;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author yyf
 * @description 统一线程池命令行工具（适配UnifiedTPRegulator）
 * 核心变更：支持多线程池管控，所有操作通过UnifiedTPRegulator统一调度
 * 命令格式调整：所有涉及线程池的操作需指定「线程池名称」（部分查询命令支持省略查全部）
 */
public class PoolCommandHandler {
    // 命令历史记录（最多100条）
    private final BlockingQueue<String> commandHistory = new LinkedBlockingDeque<>(100);
    private int historyIndex = -1;
    private volatile boolean isRunning;
    private Thread commandThread;
    // 命令提示符
    private static final String PROMPT = "yf:unified-pool> ";
    // 帮助信息常量
    private static final String HELP_HEADER = "\n======= 统一线程池命令行工具 v2.0 =======";
    private static final String HELP_FOOTER = "=======================================\n";

    public PoolCommandHandler() {
        this.isRunning = false;
    }

    /**
     * 启动命令行工具
     */
    public void start() {
        if (isRunning) {
            System.out.println("命令行工具已在运行中");
            return;
        }

        isRunning = true;
        commandThread = new Thread(this::commandLoop, "UnifiedPool-Command-Thread");
        commandThread.setDaemon(true);
        commandThread.start();
        printWelcomeMessage();
    }

    /**
     * 停止命令行工具
     */
    public void stop() {
        isRunning = false;
        if (commandThread != null) {
            commandThread.interrupt();
            commandThread = null;
        }
        System.out.println("\n统一线程池命令行工具已停止");
    }

    /**
     * 打印欢迎信息
     */
    private void printWelcomeMessage() {
        System.out.println("\n=== 统一线程池命令行工具已启动 ===");
        System.out.println("提示1: 输入 'yf help' 查看所有可用命令");
        System.out.println("提示2: 使用上下方向键查看命令历史");
        System.out.println("提示3: 可用线程池列表: " + getAvailableThreadPoolNames());
        System.out.print(PROMPT);
        System.out.flush();
    }

    /**
     * 命令循环（核心：处理输入与命令分发）
     */
    private void commandLoop() {
        Console console = System.console();
        if (console == null) {
            System.err.println("无法获取控制台，切换至简单输入模式");
            simpleInputMode();
            return;
        }

        try {
            while (isRunning) {
                // 读取带历史记录的命令
                String command = readCommandWithHistory(console);
                if (command == null || command.trim().isEmpty()) {
                    System.out.print(PROMPT);
                    System.out.flush();
                    continue;
                }

                // 保存非空命令到历史
                commandHistory.offer(command);
                historyIndex = -1;

                // 处理命令
                processCommand(command);
                System.out.print(PROMPT);
                System.out.flush();
            }
        } catch (Exception e) {
            if (isRunning) {
                System.err.println("\n命令处理异常: " + e.getMessage());
                System.out.print(PROMPT);
                System.out.flush();
            }
        }
    }

    /**
     * 带历史记录的命令读取（上下键切换）
     */
    private String readCommandWithHistory(Console console) {
        StringBuilder input = new StringBuilder();
        List<String> historyList = new ArrayList<>(commandHistory);
        Collections.reverse(historyList); // 反转：最新命令在最前

        while (true) {
            int c;
            try {
                c = System.in.read();
            } catch (Exception e) {
                return isRunning ? input.toString() : null;
            }

            // Ctrl+C 退出
            if (c == 3) {
                System.out.println("\n接收到中断信号，退出工具");
                stop();
                return null;
            }

            // 回车确认
            if (c == '\n' || c == '\r') {
                System.out.println();
                return input.toString();
            }

            // 退格删除
            if (c == 127) {
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    System.out.print("\b \b"); // 清除控制台字符
                }
                continue;
            }

            // 上下箭头（历史记录）
            if (c == 27) {
                try {
                    if (System.in.read() == 91) {
                        int key = System.in.read();
                        // 上箭头：上一条历史
                        if (key == 65) {
                            if (historyIndex < historyList.size() - 1) {
                                historyIndex++;
                                clearCurrentInput(input.length());
                                input.setLength(0);
                                if (historyIndex < historyList.size()) {
                                    String historyCmd = historyList.get(historyIndex);
                                    input.append(historyCmd);
                                    System.out.print(historyCmd);
                                }
                            }
                        }
                        // 下箭头：下一条历史
                        else if (key == 66) {
                            if (historyIndex >= 0) {
                                historyIndex--;
                                clearCurrentInput(input.length());
                                input.setLength(0);
                                if (historyIndex >= 0) {
                                    String historyCmd = historyList.get(historyIndex);
                                    input.append(historyCmd);
                                    System.out.print(historyCmd);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略控制字符读取错误
                }
                continue;
            }

            // 普通字符输入
            input.append((char) c);
            System.out.print((char) c);
        }
    }

    /**
     * 清除当前输入内容（退格时用）
     */
    private void clearCurrentInput(int length) {
        for (int i = 0; i < length; i++) {
            System.out.print("\b \b");
        }
    }

    /**
     * 简单输入模式（Console不可用时降级）
     */
    private void simpleInputMode() {
        Scanner scanner = new Scanner(System.in);
        while (isRunning) {
            try {
                System.out.print(PROMPT);
                System.out.flush();

                if (!scanner.hasNextLine()) {
                    break;
                }

                String command = scanner.nextLine().trim();
                if (!command.isEmpty()) {
                    commandHistory.offer(command);
                }

                processCommand(command);
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("命令处理异常: " + e.getMessage());
                }
            }
        }
        scanner.close();
    }

    /**
     * 命令核心处理逻辑
     */
    private void processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        // 命令必须以 "yf " 开头
        if (!command.startsWith("yf ")) {
            System.out.println("❌ 命令格式错误！请以 'yf ' 开头（示例：yf info pool）");
            System.out.println("   输入 'yf help' 查看完整命令列表");
            return;
        }

        // 分割命令（移除前缀 "yf "）
        String[] parts = command.substring(3).trim().split("\\s+");
        if (parts.length == 0) {
            System.out.println("❌ 请输入具体命令！输入 'yf help' 查看帮助");
            return;
        }

        try {
            switch (parts[0]) {
                case "info":
                    handleInfoCommand(parts);
                    break;
                case "change":
                    handleChangeCommand(parts);
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    System.out.println("✅ 正在退出命令行工具...");
                    stop();
                    break;
                default:
                    System.out.println("❌ 未知命令: " + parts[0]);
                    System.out.println("   输入 'yf help' 查看所有可用命令");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ 数字格式错误: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ 参数错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 命令执行失败: " + e.getMessage());
        }
    }

    // ============================= 信息查询命令处理 =============================
    private void handleInfoCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ info命令需指定查询类型！可用类型：pool, worker, taskNum, partitionTaskNum");
            System.out.println("   示例：yf info pool（查所有线程池）、yf info worker tp1（查tp1的线程状态）");
            return;
        }

        // 解析查询类型和线程池名称（可选）
        String infoType = parts[1];
        String tpName = parts.length >= 3 ? parts[2] : null;

        // 校验线程池名称（如果指定）
        if (tpName != null && !UnifiedTPRegulator.getAllThreadPoolNames().contains(tpName)) {
            throw new IllegalArgumentException("线程池名称不存在！可用线程池：" + getAvailableThreadPoolNames());
        }

        switch (infoType) {
            case "pool":
                printThreadPoolInfo(tpName); // 查线程池基本信息（tpName为null查所有）
                break;
            case "worker":
                if (tpName == null) {
                    throw new IllegalArgumentException("查询线程状态需指定线程池名称！示例：yf info worker tp1");
                }
                printThreadsInfo(tpName); // 查指定线程池的线程状态
                break;
            case "taskNum":
                printTaskNums(tpName); // 查队列总任务数（tpName为null查所有）
                break;
            case "partitionTaskNum":
                if (tpName == null) {
                    throw new IllegalArgumentException("查询分区任务数需指定线程池名称！示例：yf info partitionTaskNum tp1");
                }
                printPartitionTaskNums(tpName); // 查指定线程池的分区任务数
                break;
            default:
                System.out.println("❌ 未知的info查询类型: " + infoType);
                System.out.println("   可用类型：pool, worker, taskNum, partitionTaskNum");
        }
    }

    // 打印线程池基本信息（支持单池/所有）
    private void printThreadPoolInfo(String tpName) {
        System.out.println("\n================ 线程池基本信息 ================");
        List<PoolInfo> poolInfos = tpName == null
                ? UnifiedTPRegulator.getAllThreadPoolInfo()
                : Collections.singletonList(UnifiedTPRegulator.getThreadPoolInfo(tpName));

        if (poolInfos.isEmpty()) {
            System.out.println("⚠️  无已注册的线程池");
            System.out.println("===============================================");
            return;
        }

        for (PoolInfo info : poolInfos) {
            System.out.println("线程池名称: " + info.getPoolName());
            System.out.println("核心线程数: " + info.getCoreNums());
            System.out.println("最大线程数: " + info.getMaxNums());
            System.out.println("线程存活时间: " + info.getAliveTime() + "ms");
            System.out.println("线程名称前缀: " + info.getThreadName());
            System.out.println("是否允许核心线程销毁: " + info.isCoreDestroy());
            System.out.println("是否守护线程: " + info.isDaemon());
            System.out.println("使用队列: " + info.getQueueName());
            System.out.println("拒绝策略: " + info.getRejectStrategyName());
            System.out.println("-----------------------------------------------");
        }
        System.out.println("===============================================");
    }

    // 打印指定线程池的线程状态（核心/额外线程分开）
    private void printThreadsInfo(String tpName) {
        System.out.println("\n================ " + tpName + " 线程状态 ================");
        Map<String, Map<Thread.State, Integer>> threadsInfo = UnifiedTPRegulator.getThreadsInfo(tpName);

        // 打印核心线程状态
        System.out.println("【核心线程】");
        printThreadStateMap(threadsInfo.get("CORE"));
        // 打印额外线程状态
        System.out.println("【额外线程】");
        printThreadStateMap(threadsInfo.get("EXTRA"));
        System.out.println("=================================================");
    }

    // 辅助：打印线程状态统计（State -> 数量）
    private void printThreadStateMap(Map<Thread.State, Integer> stateMap) {
        if (stateMap == null || stateMap.isEmpty()) {
            System.out.println("  无活跃线程");
            return;
        }
        for (Map.Entry<Thread.State, Integer> entry : stateMap.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + "个");
        }
    }

    // 打印队列总任务数（支持单池/所有）
    private void printTaskNums(String tpName) {
        System.out.println("\n================ 队列任务数统计 ================");
        List<String> tpNames = tpName == null
                ? UnifiedTPRegulator.getAllThreadPoolNames()
                : Collections.singletonList(tpName);

        if (tpNames.isEmpty()) {
            System.out.println("⚠️  无已注册的线程池");
            System.out.println("===============================================");
            return;
        }

        for (String name : tpNames) {
            int taskNum = UnifiedTPRegulator.getTaskNums(name);
            System.out.println(name + ": " + taskNum + "个任务");
        }
        System.out.println("===============================================");
    }

    // 打印指定线程池的分区任务数
    private void printPartitionTaskNums(String tpName) {
        System.out.println("\n================ " + tpName + " 分区任务数 ================");
        Map<Integer, Integer> partitionTaskMap = UnifiedTPRegulator.getPartitionTaskNums(tpName);

        if (partitionTaskMap.isEmpty()) {
            System.out.println("⚠️  无分区任务数据");
            System.out.println("=================================================");
            return;
        }

        int total = 0;
        for (Map.Entry<Integer, Integer> entry : partitionTaskMap.entrySet()) {
            System.out.println("分区" + entry.getKey() + ": " + entry.getValue() + "个任务");
            total += entry.getValue();
        }
        System.out.println("-----------------------------------------------");
        System.out.println("总任务数: " + total + "个");
        System.out.println("=================================================");
    }

    // ============================= 配置修改命令处理 =============================
    private void handleChangeCommand(String[] parts) throws Exception {
        if (parts.length < 2) {
            System.out.println("❌ change命令需指定修改类型！可用类型：worker, queue, rejectstrategy");
            System.out.println("   示例：yf change worker tp1 -coreNums 5、yf change queue tp1 linked");
            return;
        }

        String changeType = parts[1];
        // 所有修改命令必须指定线程池名称（parts[2]）
        if (parts.length < 3) {
            throw new IllegalArgumentException("修改命令需指定线程池名称！示例：yf change " + changeType + " tp1 ...");
        }
        String tpName = parts[2];

        // 校验线程池名称
        if (!UnifiedTPRegulator.getAllThreadPoolNames().contains(tpName)) {
            throw new IllegalArgumentException("线程池名称不存在！可用线程池：" + getAvailableThreadPoolNames());
        }

        switch (changeType) {
            case "worker":
                handleChangeWorker(tpName, parts); // 修改线程参数
                break;
            case "queue":
                handleChangeQueue(tpName, parts); // 修改队列
                break;
            case "rejectstrategy":
                handleChangeRejectStrategy(tpName, parts); // 修改拒绝策略
                break;
            default:
                System.out.println("❌ 未知的change修改类型: " + changeType);
                System.out.println("   可用类型：worker, queue, rejectstrategy");
        }
    }

    // 修改线程池Worker参数（核心数、最大数等）
    private void handleChangeWorker(String tpName, String[] parts) {
        // 参数格式：yf change worker tp1 -coreNums 5 -maxNums 10
        if (parts.length < 4) {
            throw new IllegalArgumentException("需指定至少一个Worker参数！示例：yf change worker " + tpName + " -coreNums 5");
        }

        // 解析参数（-key value 格式）
        Map<String, String> params = parseWorkerParams(parts, 3); // 从索引3开始是参数（parts[0]=change,1=worker,2=tpName）

        // 转换参数类型
        Integer coreNums = parseIntegerParam(params, "coreNums");
        Integer maxNums = parseIntegerParam(params, "maxNums");
        Boolean coreDestroy = parseBooleanParam(params, "coreDestroy");
        Integer aliveTime = parseIntegerParam(params, "aliveTime");
        Boolean isDaemon = parseBooleanParam(params, "isUseDaemonThread");

        // 检查是否有有效参数
        if (coreNums == null && maxNums == null && coreDestroy == null && aliveTime == null && isDaemon == null) {
            throw new IllegalArgumentException("未识别到有效参数！可用参数：-coreNums, -maxNums, -coreDestroy, -aliveTime, -isUseDaemonThread");
        }

        // 调用UnifiedTPRegulator修改
        boolean success = UnifiedTPRegulator.changeWorkerParams(tpName, coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
        if (success) {
            System.out.println("✅ " + tpName + " 线程参数修改成功！");
        } else {
            System.err.println("❌ " + tpName + " 线程参数修改失败（可能参数不合法，如maxNums < coreNums）");
        }
    }

    // 修改线程池队列
    private void handleChangeQueue(String tpName, String[] parts) throws Exception {
        // 参数格式：yf change queue tp1 linked
        if (parts.length < 4) {
            throw new IllegalArgumentException("需指定队列名称！示例：yf change queue " + tpName + " linked");
        }
        String queueName = parts[3];

        // 校验队列名称
        if (!UnifiedTPRegulator.getAllQueueName().contains(queueName)) {
            throw new IllegalArgumentException("队列名称不存在！可用队列：" + getAvailableQueues());
        }

        // 创建新队列实例（复用原队列容量）
        Partition<?> oldPartition = UnifiedTPRegulator.getResource(tpName).getPartition();
        Class<?> queueClass = PartiResourceManager.getResources().get(queueName);
        Partition<Runnable> newQueue = (Partition<Runnable>) queueClass.getConstructor(Integer.class).newInstance(oldPartition.getCapacity());

        // 调用UnifiedTPRegulator修改
        boolean success = UnifiedTPRegulator.changeQueue(tpName, newQueue);
        if (success) {
            System.out.println("✅ " + tpName + " 队列修改成功！新队列：" + queueName);
        } else {
            System.err.println("❌ " + tpName + " 队列修改失败！");
        }
    }

    // 修改线程池拒绝策略
    private void handleChangeRejectStrategy(String tpName, String[] parts) throws Exception {
        // 参数格式：yf change rejectstrategy tp1 callerRuns
        if (parts.length < 4) {
            throw new IllegalArgumentException("需指定拒绝策略名称！示例：yf change rejectstrategy " + tpName + " callerRuns");
        }
        String strategyName = parts[3];

        // 校验拒绝策略名称
        if (!UnifiedTPRegulator.getAllRejectStrategyName().contains(strategyName)) {
            throw new IllegalArgumentException("拒绝策略不存在！可用策略：" + getAvailableRejectStrategies());
        }

        // 创建新拒绝策略实例
        Class<?> strategyClass = RSResourceManager.getResources().get(strategyName);
        RejectStrategy newStrategy = (RejectStrategy) strategyClass.getConstructor().newInstance();

        // 调用UnifiedTPRegulator修改
        boolean success = UnifiedTPRegulator.changeRejectStrategy(tpName, newStrategy, strategyName);
        if (success) {
            System.out.println("✅ " + tpName + " 拒绝策略修改成功！新策略：" + strategyName);
        } else {
            System.err.println("❌ " + tpName + " 拒绝策略修改失败！");
        }
    }

    // ============================= 辅助工具方法 =============================
    // 解析Worker参数（-key value格式）
    private Map<String, String> parseWorkerParams(String[] parts, int startIndex) {
        Map<String, String> params = new HashMap<>();
        for (int i = startIndex; i < parts.length; i += 2) {
            // 检查是否有完整的key-value
            if (i + 1 >= parts.length) {
                System.out.println("⚠️ 参数 " + parts[i] + " 缺少值，已忽略");
                break;
            }

            String key = parts[i].startsWith("-") ? parts[i].substring(1) : parts[i];
            String value = parts[i + 1];

            // 校验参数合法性
            if (!Arrays.asList("coreNums", "maxNums", "coreDestroy", "aliveTime", "isUseDaemonThread").contains(key)) {
                System.out.println("⚠️ 未知参数 " + parts[i] + "，已忽略");
                continue;
            }

            params.put(key, value);
        }
        return params;
    }

    // 解析整数参数（如coreNums、maxNums）
    private Integer parseIntegerParam(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }

        try {
            int value = Integer.parseInt(params.get(key));
            if (value < 0) {
                System.out.println("⚠️ 参数 " + key + " 不能为负数，已忽略");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("⚠️ 参数 " + key + " 必须是整数，已忽略");
            return null;
        }
    }

    // 解析布尔参数（如coreDestroy、isUseDaemonThread）
    private Boolean parseBooleanParam(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }

        String value = params.get(key).toLowerCase();
        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        }

        System.out.println("⚠️ 参数 " + key + " 必须是 true/false，已忽略");
        return null;
    }

    // 获取可用线程池名称（逗号分隔）
    private String getAvailableThreadPoolNames() {
        List<String> tpNames = UnifiedTPRegulator.getAllThreadPoolNames();
        return tpNames.isEmpty() ? "无" : String.join(", ", tpNames);
    }

    // 获取可用队列名称（逗号分隔）
    private String getAvailableQueues() {
        List<String> queueNames = UnifiedTPRegulator.getAllQueueName();
        return queueNames.isEmpty() ? "无" : String.join(", ", queueNames);
    }

    // 获取可用拒绝策略名称（逗号分隔）
    private String getAvailableRejectStrategies() {
        List<String> strategyNames = UnifiedTPRegulator.getAllRejectStrategyName();
        return strategyNames.isEmpty() ? "无" : String.join(", ", strategyNames);
    }

    // ============================= 帮助信息 =============================
    private void printHelp() {
        StringBuilder help = new StringBuilder();
        help.append(HELP_HEADER).append("\n");
        help.append("说明：所有命令需以 'yf ' 开头，线程池名称可通过 'yf info pool' 查看\n\n");

        // 1. 信息查询命令
        help.append("【1. 信息查询命令】\n");
        help.append("  yf info pool [tpName]        - 查看线程池基本信息（无tpName查所有）\n");
        help.append("                                 示例：yf info pool、yf info pool tp1\n");
        help.append("  yf info worker <tpName>      - 查看指定线程池的线程状态（核心/额外线程）\n");
        help.append("                                 示例：yf info worker tp1\n");
        help.append("  yf info taskNum [tpName]     - 查看队列总任务数（无tpName查所有）\n");
        help.append("                                 示例：yf info taskNum、yf info taskNum tp1\n");
        help.append("  yf info partitionTaskNum <tpName> - 查看指定线程池的分区任务数\n");
        help.append("                                     示例：yf info partitionTaskNum tp1\n\n");

        // 2. 配置修改命令
        help.append("【2. 配置修改命令】\n");
        help.append("  yf change worker <tpName> [参数]  - 修改线程参数（参数可选，至少一个）\n");
        help.append("                                     参数：-coreNums [数字]（核心线程数）\n");
        help.append("                                           -maxNums [数字]（最大线程数）\n");
        help.append("                                           -coreDestroy [true/false]（核心线程是否可销毁）\n");
        help.append("                                           -aliveTime [数字]（线程存活时间，ms）\n");
        help.append("                                           -isUseDaemonThread [true/false]（是否守护线程）\n");
        help.append("                                     示例：yf change worker tp1 -coreNums 5 -maxNums 10\n");
        help.append("\n");
        help.append("  yf change queue <tpName> <queueName>  - 修改指定线程池的队列\n");
        help.append("                                          可用队列：").append(getAvailableQueues()).append("\n");
        help.append("                                          示例：yf change queue tp1 linked\n");
        help.append("\n");
        help.append("  yf change rejectstrategy <tpName> <strategyName>  - 修改指定线程池的拒绝策略\n");
        help.append("                                                     可用策略：").append(getAvailableRejectStrategies()).append("\n");
        help.append("                                                     示例：yf change rejectstrategy tp1 callerRuns\n\n");

        // 3. 其他命令
        help.append("【3. 其他命令】\n");
        help.append("  yf help             - 显示此帮助信息\n");
        help.append("  yf exit             - 退出命令行工具\n");
        help.append("  上下方向键          - 查看命令历史记录\n");

        help.append(HELP_FOOTER);
        System.out.print(help.toString());
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPool pool = new ThreadPool(
                5,
                10,
                "GC-ThreadPool",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
        new PoolCommandHandler().start();
        Thread.sleep(100000000000000L);
    }
}
