package com.campus.trading.config;

import com.campus.trading.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器
 *  - 握手时通过 query 参数 ?token=xxx 完成登录校验
 *  - 服务端主动向在线用户推送新消息 (由 ChatService 调用 pushTo)
 *  - 客户端可发 {"type":"ping"} 心跳, 服务端回 {"type":"pong"}
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Autowired
    private JwtUtil jwtUtil;

    public static void pushTo(Long userId, String json) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception ignore) {
                    // 推送失败忽略, 客户端会通过轮询兜底
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignore) {
            }
            return;
        }
        SESSIONS.put(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            if (message.getPayload().contains("\"ping\"")) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.values().remove(session);
    }

    private Long resolveUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        for (String kv : query.split("&")) {
            String[] pair = kv.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                String token = pair[1];
                if (jwtUtil.validate(token)) {
                    return jwtUtil.parseUserId(token);
                }
            }
        }
        return null;
    }
}
