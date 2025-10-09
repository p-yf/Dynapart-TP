package com.yf.test_or_explore_springboot_integration.ai_explore.schedule;

import com.yf.test_or_explore_springboot_integration.ai_explore.config.CentralConfig;
import com.yf.test_or_explore_springboot_integration.ai_explore.tools.RegulateTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import static com.yf.test_or_explore_springboot_integration.ai_explore.utils.Utils.getLoad;


/**
 * @author yyf
 * @date 2025/10/8 18:20
 * @description
 */
@Component
@ConditionalOnProperty(prefix = "yf.thread-pool.ai", name = "enabled", havingValue = "true")
public class RegulateRequestToModel {

    @Resource
    private ChatClient chatClient;

    @Resource
    private VectorStore vectorStore;

    @Value("${yf.thread-pool.ai.training}")
    private boolean isTrainingMode;



    @Scheduled(fixedDelayString = "${yf.thread-pool.ai.fixedDelay}")
    public void ask(){
        System.out.println("=======~~~~~~~~~~~~开始请求模型~~~~~~~~~~~~=============");

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                .build();


        String message;
        if(isTrainingMode){
            message = CentralConfig.TRAIN_SYSTEM_INFO;
        }else{
            message = CentralConfig.NOT_TRAIN_SYSTEM_INFO;
        }
        String s = chatClient.prompt().system(message)
                .user(getLoad().toString()).tools(new RegulateTools()).advisors(advisor).call().content();
        System.out.println(s);
    }




}
