package com.yf.test_or_explore_springboot_integration.ai_explore.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.test_or_explore_springboot_integration.ai_explore.tools.RegulateInformation;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyf
 * @date 2025/10/9 16:33
 * @description
 */
@Component
@ConditionalOnProperty(prefix = "yf.thread-pool.ai", name ="enabled",havingValue = "true")
public class Utils {


    private static VectorStore vectorStore;
    private static ResourceLoader resourceLoader;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TokenTextSplitter textSplitter = new TokenTextSplitter();

    // 注入Spring管理的Bean到静态变量
    @Autowired
    public void setDependencies(VectorStore vectorStore, ResourceLoader resourceLoader) {
        Utils.vectorStore = vectorStore;
        Utils.resourceLoader = resourceLoader;
    }

    public static Map<String, String> getLoad(){
        Map<String, String> map = new HashMap<>();
        double memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100.0 / Runtime.getRuntime().maxMemory();;
        map.put("memoryUsage", String.valueOf(memoryUsage));
        for(Map.Entry<String, ThreadPool> entry: UnifiedTPRegulator.getResources().entrySet()){
            ThreadPool threadPool = entry.getValue();
            String name = entry.getKey();
            int taskNums = threadPool.getPartition().getEleNums();
            map.put(name+":taskNums", String.valueOf(taskNums));
            Integer capacity = threadPool.getPartition().getCapacity();
            map.put(name+":queueCapacity", String.valueOf(capacity));
        }
        return map;
    }

    /**
     * 将调控信息写入RAG系统（向量数据库和对应类型文件）
     * @param type 线程池类型（cpu/io）
     * @param information 调控信息对象
     */
    public static void writeToRag(String type, RegulateInformation information) {
        if (type == null || !("cpu".equals(type) || "io".equals(type)) || information == null) {
            throw new IllegalArgumentException("无效的类型或调控信息");
        }

        File tempFile = null;
        try {
            // 1. 将调控信息转换为JSON字符串
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(information);

            // 2. 创建临时文件并写入JSON数据
            tempFile = File.createTempFile("regulate-"+type+"-", ".json");
            Files.write(tempFile.toPath(), jsonContent.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 3. 将临时文件内容写入向量数据库
            TextReader textReader = new TextReader(new FileSystemResource(tempFile));
            textReader.setCharset(StandardCharsets.UTF_8);
            List<Document> documents = textSplitter.transform(textReader.read());
            vectorStore.add(documents);

            // 4. 将JSON数据追加到对应的类型文件（cpu.txt或io.txt）
            appendToTypeFile(type, jsonContent);


        } catch (Exception e) {
            throw new RuntimeException("处理调控信息时发生错误", e);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit(); // 确保程序退出时删除
                }
            }
        }
    }

    /**
     * 将JSON内容追加到对应的类型文件
     * @param type 线程池类型（cpu/io）
     * @param jsonContent 要追加的JSON内容
     */
    private static void appendToTypeFile(String type, String jsonContent) throws IOException {
        String fileName = type + ".txt";
        Resource resource = resourceLoader.getResource("classpath:" + fileName);

        // 构建追加内容（添加时间戳和分隔符）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = "// 调控时间: " + LocalDateTime.now().format(formatter) + "\n";
        String contentToAppend = timestamp + jsonContent + "\n\n";

        // 处理文件写入
        if (resource.exists() && resource.isFile()) {
            // 直接写入现有文件
            Files.write(Path.of(resource.getURI()),
                    contentToAppend.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } else {
            // 资源不存在或在JAR包内，写入外部目录
            File externalDir = new File("src/main/resources");
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            File externalFile = new File(externalDir, fileName);
            Files.write(externalFile.toPath(),
                    contentToAppend.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
}
