package com.yf.monitor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author yyf
 * @description
 */
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

    // 向所有连接的客户端推送线程池信息
    public static void broadcastThreadPoolInfo(Map<String, Map<Thread.State, Integer>> threadPoolInfo) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            // 将线程池信息转换为JSON字符串
            String jsonMessage = objectMapper.writeValueAsString(threadPoolInfo);
            TextMessage message = new TextMessage(jsonMessage);

            // 向每个会话发送消息
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("推送线程池信息失败: " + e.getMessage());
        }
    }

    public static void broadcastTaskNums(int nums) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            // 将线程池信息转换为JSON字符串
            String jsonMessage = objectMapper.writeValueAsString(nums);
            TextMessage message = new TextMessage(jsonMessage);

            // 向每个会话发送消息
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("推送任务数量失败: " + e.getMessage());
        }
    }
}
