package com.foodcommunity.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 在线会话注册表（userId → session）。
 * 同一用户多端登录时后连的覆盖先连的（毕设单端假设）。
 */
@Slf4j
@Component
public class WsSessionRegistry {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        WebSocketSession old = sessions.put(userId, session);
        if (old != null && old.isOpen()) {
            try {
                old.close();
            } catch (IOException ignored) {
                // 旧连接关闭失败不影响新连接
            }
        }
    }

    public void unregister(Long userId, WebSocketSession session) {
        sessions.remove(userId, session);
    }

    public boolean isOnline(Long userId) {
        WebSocketSession s = sessions.get(userId);
        return s != null && s.isOpen();
    }

    /** 在线则推送文本，离线返回 false（消息已落库，稍后由接收方拉取） */
    public boolean sendTo(Long userId, String json) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
            return true;
        } catch (IOException e) {
            log.warn("WS 推送失败 to userId={}: {}", userId, e.getMessage());
            sessions.remove(userId, session);
            return false;
        }
    }
}
