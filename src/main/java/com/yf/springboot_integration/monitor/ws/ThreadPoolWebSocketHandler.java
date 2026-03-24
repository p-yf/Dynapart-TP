package com.yf.springboot_integration.monitor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @description WebSocket处理器，负责向客户端推送线程池监控数据
 */
@Slf4j
public class ThreadPoolWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的WebSocket会话
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 心跳计数器
    private static final AtomicLong heartbeatCounter = new AtomicLong(0);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket connection established, sessionId={}, current connections: {}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed, sessionId={}, status={}, remaining connections: {}", session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for sessionId={}: {}", session.getId(), exception.getMessage());
        // 尝试关闭会话
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.debug("Error closing session after transport error: {}", e.getMessage());
        }
    }

    /**
     * 移除无效会话
     */
    public static void removeInvalidSessions() {
        sessions.removeIf(session -> !session.isOpen());
    }

    /**
     * 获取活跃会话数
     */
    public static int getActiveSessionCount() {
        removeInvalidSessions();
        return sessions.size();
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

    // 广播心跳
    public static void broadcastHeartbeat() {
        broadcastWithTpName("*", "heartbeat", System.currentTimeMillis());
    }

    // 通用广播方法（包装线程池名称）
    private static void broadcastWithTpName(String tpName, String type, Object data) {
        if (sessions.isEmpty()) {
            return;
        }

        // 定期移除无效会话
        if (heartbeatCounter.incrementAndGet() % 100 == 0) {
            removeInvalidSessions();
        }

        Map<String, Object> message = new HashMap<>();
        message.put("tpName", tpName);
        message.put("type", type);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message for {}, type={}: {}", tpName, type, e.getMessage());
            return;
        }

        // 使用迭代器安全遍历，移除无效会话
        Set<WebSocketSession> deadSessions = new CopyOnWriteArraySet<>();
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                deadSessions.add(session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("Failed to send message to sessionId={}: {}", session.getId(), e.getMessage());
                deadSessions.add(session);
            }
        }
        sessions.removeAll(deadSessions);
    }
}
