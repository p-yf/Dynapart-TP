package com.yf.pool.command;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.constant.OfRejectStrategy;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import com.yf.pool.threadpool.ThreadPool;

import java.io.Console;
import java.util.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author yyf
 * @description
 * 命令：
 *yf info pool //打印线程池信息
 *yf info worker  //打印线程信息
 *yf info taskNum  //打印队列任务数量
 *yf change worker -coreNums 2 -maxNums 5 -coreDestroy true......(如果有参数没写就直接赋值为null)  //改变线程参数
 *yf change queue linked(队列名称举例)  //改变队列
 *yf change rejectstrategy callerRuns(拒绝策略名称举例)   //改变拒绝策略
 */
public class PoolCommandHandler {
    // 线程池引用
    private final ThreadPool threadPool;
    private volatile boolean isRunning;
    private Thread commandThread;
    private final BlockingQueue<String> commandHistory = new LinkedBlockingDeque<>(100);
    private int historyIndex = -1;
    private static final String PROMPT = "yf:DGA-pool> ";
    private static final String HELP_HEADER = "\n======= 线程池命令行工具 v1.0 =======";
    private static final String HELP_FOOTER = "=======================================\n";

    public PoolCommandHandler(ThreadPool threadPool) {
        this.threadPool = threadPool;
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
        commandThread = new Thread(this::commandLoop, "ThreadPool-Command-Thread");
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
        System.out.println("\n线程池命令行工具已停止");
    }

    /**
     * 打印欢迎信息
     */
    private void printWelcomeMessage() {
        System.out.println("\n=== 线程池命令行工具已启动 ===");
        System.out.println("提示: 输入 'yf help' 查看所有可用命令");
        System.out.println("提示: 使用上下方向键查看命令历史");
        System.out.print(PROMPT);
        System.out.flush();
    }

    /**
     * 命令循环，处理输入和命令执行
     */
    private void commandLoop() {
        Console console = System.console();
        if (console == null) {
            System.err.println("无法获取控制台，使用简单输入模式");
            simpleInputMode();
            return;
        }

        try {
            while (isRunning) {
                // 读取单行输入，支持历史记录
                String command = readCommandWithHistory(console);
                if (command == null || command.isEmpty()) {
                    System.out.print(PROMPT);
                    System.out.flush();
                    continue;
                }

                // 保存非空命令到历史记录
                if (!command.trim().isEmpty()) {
                    commandHistory.offer(command);
                    historyIndex = -1;  // 重置历史索引
                }

                // 处理命令
                processCommand(command);
                System.out.print(PROMPT);
                System.out.flush();
            }
        } catch (Exception e) {
            if (isRunning) {
                System.err.println("\n命令处理出错: " + e.getMessage());
                System.out.print(PROMPT);
                System.out.flush();
            }
        }
    }

    /**
     * 带历史记录功能的命令读取
     */
    private String readCommandWithHistory(Console console) {
        StringBuilder input = new StringBuilder();
        List<String> historyList = new ArrayList<>(commandHistory);
        Collections.reverse(historyList);  // 反转以便从最新开始

        while (true) {
            int c;
            try {
                c = System.in.read();
            } catch (Exception e) {
                if (isRunning) {
                    return input.toString();
                }
                return null;
            }

            // 处理Ctrl+C退出
            if (c == 3) {
                System.out.println("\n接收到中断信号，退出工具");
                stop();
                return null;
            }

            // 处理回车
            if (c == '\n' || c == '\r') {
                System.out.println();
                return input.toString();
            }

            // 处理退格
            if (c == 127) {
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    // 清除控制台字符
                    System.out.print("\b \b");
                }
                continue;
            }

            // 处理上箭头(获取历史)
            if (c == 27) {
                try {
                    if (System.in.read() == 91) {
                        int key = System.in.read();
                        // 上箭头
                        if (key == 65) {
                            if (historyIndex < historyList.size() - 1) {
                                historyIndex++;
                                // 清除当前输入
                                clearCurrentInput(input.length());
                                input.setLength(0);
                                // 加载历史命令
                                if (historyIndex < historyList.size()) {
                                    String historyCmd = historyList.get(historyIndex);
                                    input.append(historyCmd);
                                    System.out.print(historyCmd);
                                }
                            }
                        }
                        // 下箭头
                        else if (key == 66) {
                            if (historyIndex >= 0) {
                                historyIndex--;
                                // 清除当前输入
                                clearCurrentInput(input.length());
                                input.setLength(0);
                                // 加载历史命令或清空
                                if (historyIndex >= 0 && historyIndex < historyList.size()) {
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

            // 普通字符
            input.append((char) c);
            System.out.print((char) c);
        }
    }

    /**
     * 清除当前输入
     */
    private void clearCurrentInput(int length) {
        for (int i = 0; i < length; i++) {
            System.out.print("\b \b");
        }
    }

    /**
     * 简单输入模式，当无法获取Console时使用
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
                    System.err.println("命令处理出错: " + e.getMessage());
                }
            }
        }

        scanner.close();
    }

    /**
     * 处理命令
     */
    private void processCommand(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }

        // 检查是否以"yf"开头
        if (!command.startsWith("yf ")) {
            System.out.println("❌ 命令格式错误，请以 'yf ' 开头。例如: yf info pool");
            System.out.println("   输入 yf help 查看完整命令列表");
            return;
        }

        // 移除"yf "前缀，分割命令部分
        String[] parts = command.substring(3).trim().split("\\s+");
        if (parts.length == 0) {
            System.out.println("❌ 请输入具体命令。输入 yf help 查看帮助。");
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
                    System.out.println("✅ 退出命令行工具");
                    stop();
                    break;
                default:
                    System.out.println("❌ 未知命令: " + parts[0]);
                    System.out.println("   输入 yf help 查看所有可用命令");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ 数字格式错误: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ 参数错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 命令执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理信息查询命令
     */
    private void handleInfoCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ info命令需要指定参数，可用参数: pool, worker, taskNum");
            System.out.println("   示例: yf info pool");
            return;
        }

        switch (parts[1]) {
            case "pool":
                printThreadPoolInfo();
                break;
            case "worker":
                printThreadsInfo();
                break;
            case "taskNum":
                printTaskNums();
                break;
            default:
                System.out.println("❌ 未知的info参数: " + parts[1]);
                System.out.println("   可用参数: pool, worker, taskNum");
        }
    }

    /**
     * 处理修改命令
     */
    private void handleChangeCommand(String[] parts) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        if (parts.length < 2) {
            System.out.println("❌ change命令需要指定参数，可用参数: worker, queue, rejectstrategy");
            System.out.println("   示例: yf change worker -coreNums 5");
            return;
        }

        switch (parts[1]) {
            case "worker":
                handleChangeWorker(parts);
                break;
            case "queue":
                handleChangeQueue(parts);
                break;
            case "rejectstrategy":
                handleChangeRejectStrategy(parts);
                break;
            default:
                System.out.println("❌ 未知的change参数: " + parts[1]);
                System.out.println("   可用参数: worker, queue, rejectstrategy");
        }
    }

    /**
     * 处理修改worker参数的命令
     */
    private void handleChangeWorker(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ 缺少worker参数");
            System.out.println("   示例: yf change worker -coreNums 2 -maxNums 5");
            return;
        }

        // 解析参数
        Map<String, String> params = parseWorkerParams(parts);

        // 验证并转换参数类型
        Integer coreNums = parseIntegerParam(params, "coreNums");
        Integer maxNums = parseIntegerParam(params, "maxNums");
        Boolean coreDestroy = parseBooleanParam(params, "coreDestroy");
        Integer aliveTime = parseIntegerParam(params, "aliveTime");
        Boolean isDaemon = parseBooleanParam(params, "isDaemon");

        // 检查是否有至少一个参数被设置
        if (coreNums == null && maxNums == null && coreDestroy == null
                && aliveTime == null && isDaemon == null) {
            System.out.println("❌ 没有指定任何有效的worker参数");
            System.out.println("   可用参数: -coreNums, -maxNums, -coreDestroy, -aliveTime, -isDaemon");
            return;
        }

        // 调用修改方法
        changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
    }

    /**
     * 解析worker参数
     */
    private Map<String, String> parseWorkerParams(String[] parts) {
        Map<String, String> params = new HashMap<>();

        for (int i = 2; i < parts.length; i += 2) {
            if (i + 1 >= parts.length) {
                System.out.println("⚠️ 参数 " + parts[i] + " 缺少值，已忽略");
                break;
            }

            String key = parts[i].startsWith("-") ? parts[i].substring(1) : parts[i];
            String value = parts[i + 1];

            // 验证参数名是否有效
            if (!Arrays.asList("coreNums", "maxNums", "coreDestroy", "aliveTime", "isDaemon").contains(key)) {
                System.out.println("⚠️ 未知参数 " + parts[i] + "，已忽略");
                continue;
            }

            params.put(key, value);
        }

        return params;
    }

    /**
     * 处理修改队列的命令
     */
    private void handleChangeQueue(String[] parts) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        if (parts.length < 3) {
            System.out.println("❌ 请指定队列名称");
            System.out.println("   示例: yf change queue linked");
            System.out.println("   可用队列: " + getAvailableQueues());
            return;
        }

        String queueName = parts[2];
        // 简单验证队列名称是否存在
        if (!OfQueue.TASK_QUEUE_MAP.containsKey(queueName)) {
            System.out.println("❌ 未知的队列名称: " + queueName);
            System.out.println("   可用队列: " + getAvailableQueues());
            return;
        }

        changeQueue(queueName);
    }

    /**
     * 处理修改拒绝策略的命令
     */
    private void handleChangeRejectStrategy(String[] parts) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        if (parts.length < 3) {
            System.out.println("❌ 请指定拒绝策略名称");
            System.out.println("   示例: yf change rejectstrategy callerRuns");
            System.out.println("   可用策略: " + getAvailableRejectStrategies());
            return;
        }

        String strategyName = parts[2];
        // 简单验证策略名称是否存在
        if (!OfRejectStrategy.REJECT_STRATEGY_MAP.containsKey(strategyName)) {
            System.out.println("❌ 未知的拒绝策略: " + strategyName);
            System.out.println("   可用策略: " + getAvailableRejectStrategies());
            return;
        }

        changeRejectStrategy(strategyName);
    }

    /**
     * 解析整数参数
     */
    private Integer parseIntegerParam(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }

        try {
            return Integer.parseInt(params.get(key));
        } catch (NumberFormatException e) {
            System.out.println("⚠️ 参数 " + key + " 必须是整数，已忽略");
            return null;
        }
    }

    /**
     * 解析布尔参数
     */
    private Boolean parseBooleanParam(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }

        String value = params.get(key).toLowerCase();
        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        }

        System.out.println("⚠️ 参数 " + key + " 必须是 true 或 false，已忽略");
        return null;
    }

    /**
     * 获取可用队列列表
     */
    private String getAvailableQueues() {
        return String.join(", ", OfQueue.TASK_QUEUE_MAP.keySet());
    }

    /**
     * 获取可用拒绝策略列表
     */
    private String getAvailableRejectStrategies() {
        return String.join(", ", OfRejectStrategy.REJECT_STRATEGY_MAP.keySet());
    }

    /**
     * 打印帮助信息
     */
    private void printHelp() {
        StringBuilder help = new StringBuilder();
        help.append(HELP_HEADER).append("\n");

        help.append("所有命令都需要以 'yf ' 开头\n\n");

        help.append("1. 查看信息:\n");
        help.append("   yf info pool        - 打印线程池信息\n");
        help.append("   yf info worker      - 打印线程信息\n");
        help.append("   yf info taskNum     - 打印队列任务数量\n\n");

        help.append("2. 修改配置:\n");
        help.append("   yf change worker [参数]  - 改变线程参数，参数可选:\n");
        help.append("       -coreNums [数字]    - 核心线程数\n");
        help.append("       -maxNums [数字]     - 最大线程数\n");
        help.append("       -coreDestroy [true/false] - 是否允许核心线程销毁\n");
        help.append("       -aliveTime [数字]   - 线程存活时间\n");
        help.append("       -isDaemon [true/false] - 是否为守护线程\n");
        help.append("   示例: yf change worker -coreNums 2 -maxNums 5\n\n");

        help.append("   yf change queue [队列名称]  - 改变队列\n");
        help.append("   可用队列: ").append(getAvailableQueues()).append("等\n");
        help.append("   示例: yf change queue linked\n\n");

        help.append("   yf change rejectstrategy [策略名称]  - 改变拒绝策略\n");
        help.append("   可用策略: ").append(getAvailableRejectStrategies()).append("等\n");
        help.append("   示例: yf change rejectstrategy callerRuns\n\n");

        help.append("3. 其他命令:\n");
        help.append("   yf help             - 显示此帮助信息\n");
        help.append("   yf exit             - 退出命令行工具\n");
        help.append("   上下方向键          - 查看命令历史\n");

        help.append(HELP_FOOTER);
        System.out.print(help.toString());
    }

    // 以下是修改与查看参数的方法
    private void printThreadPoolInfo() {
        System.out.println("================线程池信息==============");
        System.out.println(threadPool.getThreadPoolInfo());
        System.out.println("=======================================");
    }

    private void printThreadsInfo() {
        System.out.println("===============线程状态信息===============");
        System.out.println(threadPool.getThreadsInfo());
        System.out.println("=======================================");

    }

    private void printTaskNums() {
        System.out.println("================任务数量================");
        System.out.println(threadPool.getTaskNums());
        System.out.println("=======================================");
    }

    private void changeWorkerParams(Integer coreNums, Integer maxNums, Boolean coreDestroy, Integer aliveTime, Boolean isDaemon) {
        if(threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon)){
            System.out.println("✅ 修改成功");
        }else{
            System.err.println("❌ 修改失败");
        }
    }

    private void changeQueue(String qName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        TaskQueue newQ = (TaskQueue) OfQueue.TASK_QUEUE_MAP.get(qName).getConstructor(Integer.class).newInstance(threadPool.getTaskQueue().getCapacity());
        if(threadPool.changeQueue(newQ, qName)){
            System.out.println("✅ 修改成功");
        }else{
            System.err.println("❌ 修改失败");
        }
    }

    private void changeRejectStrategy(String rjName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RejectStrategy newQ = (RejectStrategy) OfRejectStrategy.REJECT_STRATEGY_MAP.get(rjName).getConstructor().newInstance();
        if(threadPool.changeRejectStrategy(newQ,rjName)){
            System.out.println("✅ 修改成功");
        }else{
            System.err.println("❌ 修改失败");
        }
    }
}
