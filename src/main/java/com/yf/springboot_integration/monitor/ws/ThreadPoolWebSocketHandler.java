package com.yf.springboot_integration.monitor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yf.common.constant.Logo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author yyf
 * @description
 */
@Slf4j
public class ThreadPoolWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的WebSocket会话
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 新连接建立时添加会话
        sessions.add(session);
        System.out.println("新的WebSocket连接建立，当前连接数: " + sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 连接关闭时移除会话
        sessions.remove(session);
        System.out.println("WebSocket连接关闭，当前连接数: " + sessions.size());
    }

    // 新增：按线程池名称广播线程状态
    public static void broadcastThreadPoolInfo(String tpName, Map<String, Map<Thread.State, Integer>> threadInfo) {
        broadcastWithTpName(tpName, "threadInfo", threadInfo);
    }

    // 新增：按线程池名称广播任务数量
    public static void broadcastTaskNums(String tpName, int nums) {
        broadcastWithTpName(tpName, "taskNums", nums);
    }

    // 新增：按线程池名称广播分区任务数量
    public static void broadcastPartitionTaskNums(String tpName, Map<Integer, Integer> partitionNums) {
        broadcastWithTpName(tpName, "partitionTaskNums", partitionNums);
    }

    // 通用广播方法（包装线程池名称）
    private static void broadcastWithTpName(String tpName, String type, Object data) {
        if (sessions.isEmpty()) return;
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("tpName", tpName);
            message.put("type", type);
            message.put("data", data);
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("推送线程池[" + tpName + "]信息失败: " + e.getMessage());
        }
    }
}
