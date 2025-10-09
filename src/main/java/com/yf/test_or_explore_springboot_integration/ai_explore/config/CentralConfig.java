package com.yf.test_or_explore_springboot_integration.ai_explore.config;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * @author yyf
 * @date 2025/10/8 18:13
 * @description
 */
@ConditionalOnProperty(prefix = "yf.thread-pool.ai", name ="enabled",havingValue = "true")
@Configuration
public class CentralConfig {



    @Value("classpath:base_knowledge.txt")
    private Resource knowledge;
    @Value("classpath:io.txt")
    private Resource io;
    @Value("classpath:cpu.txt")
    private Resource cpu;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${spring.ai.vectorstore.redis.prefix}")
    private String redisPrefix;

    public static final String NOT_TRAIN_SYSTEM_INFO =
            "你是个线程池调控参数的非训练模式下的助手，你需要根据用户发送给你的负载情况和知识文档决定是否进行调控，以下是负载信息的说明" +
            "负载信息是key value都是String类型的Map，获得的是内存使用率和各个线程池负载," +
            "除了memoryUsage，其他key冒号前面的是线程池的名字\n";

    public static final String TRAIN_SYSTEM_INFO =
            "你是个线程池调控参数的训练模式下的助手，你需要根据用户发送给你的负载情况和知识文档决定是否进行调控，以下是负载信息的说明" +
            "负载信息是key value都是String类型的Map，获得的是内存使用率和各个线程池负载," +
            "除了memoryUsage，其他key冒号前面的是线程池的名字；" +
            "另外如果你进行了调控，你还需要调用writeToRag方法\n";

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

    @PostConstruct
    public void init(){
        //0删除之前的rag文档,按照前缀删除
        String pattern = redisPrefix + "*";

        // 获取所有匹配前缀的键
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            // 批量删除匹配的键
            Long deleted = stringRedisTemplate.delete(keys);
            System.out.println("删除了" + deleted + "个前缀为" + redisPrefix + "的文档");
        }

        //1 读取文件
        TextReader knowledgeTextReader = new TextReader(knowledge);
        knowledgeTextReader.setCharset(Charset.defaultCharset());

        TextReader ioTextReader = new TextReader(io);
        ioTextReader.setCharset(Charset.defaultCharset());

        TextReader cpuTextReader = new TextReader(cpu);
        cpuTextReader.setCharset(Charset.defaultCharset());

        //2 文件内容转换为向量(开启分词）
        List<Document> knledgeList = new TokenTextSplitter().transform(knowledgeTextReader.read());
        List<Document> ioList = new TokenTextSplitter().transform(ioTextReader.read());
        List<Document> cpuList = new TokenTextSplitter().transform(cpuTextReader.read());

        //3 写入向量数据库
        vectorStore.add(knledgeList);
        vectorStore.add(ioList);
        vectorStore.add(cpuList);
    }



}
