package com.yf.test_or_explore_springboot_integration.ai_explore.tools;

import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.test_or_explore_springboot_integration.ai_explore.utils.Utils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author yyf
 * @date 2025/10/9 14:38
 * @description
 */
@Component
public class RegulateTools {

    @Value("${yf.thread-pool.ai.training}")
    private boolean isTrainingMode;

    @Tool(description = "获取线程池的类型，建议调控之前先了解类型。类型代表着线程池中任务的类型，类型主要分为两种1 cpu，2 io 如果不是这两种，那么就代表线程池没有固定哪种类型。")
    public String getPoolType(String poolName){
        System.out.println("~~~~~~~~~~~·使用了");
        return UnifiedTPRegulator.getResource(poolName).getType();
    }


    @Tool(description = "调控线程数量，返回值代表成功与否")
    public boolean regulateWorker(String poolName,Integer coreNums, Integer maxNums,Integer aliveTime) {
        System.out.println("~~~~~~~~~~~·使用了");
        return UnifiedTPRegulator.changeWorkerParams(poolName, coreNums, maxNums, null, aliveTime, null);
    }

    @Tool(description = "调控队列长短")
    public void regulateQueue(String poolName,Integer queueCapacity) {
        System.out.println("~~~~~~~~~~~·使用了");
        UnifiedTPRegulator.changeQueueCapacity(poolName, queueCapacity);
    }

    @Tool(description = "将调控的信息写入rag数据库，第二个参数的load成员代表这次用户发送给你的的负载信息；" +
            "toolName代表你使用的调控工具名称没有使用就不用写；" +
            "params的key代表你设置的参数名，value代表你设置的具体的值，没有使用调控工具就不用写")
    public void writeToRag(String type,RegulateInformation information){
        System.out.println("~~~~~~~~~~~·使用了writeToRag");
        if (type == null || !("cpu".equals(type) || "io".equals(type)) || information == null) {
            return;
        }
        // 仅训练模式开启时写入
        if (!isTrainingMode) {
            return;
        }
        Utils.writeToRag(type,information);
    }
}
