package com.foodcommunity.ws;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodcommunity.config.WebSocketConfig;
import com.foodcommunity.dto.ChatInbound;
import com.foodcommunity.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 一对一聊天 WebSocket 处理器。
 * 入站：{toUserId, content} → 落库 + 对方在线则实时推送 {type:'chat', message:{...}}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WsSessionRegistry registry;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long userId = currentUserId(session);
        if (userId != null) {
            registry.register(userId, session);
            log.info("WS 连接建立 userId={}", userId);
        } else {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long fromId = currentUserId(session);
        if (fromId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        ChatInbound inbound;
        try {
            inbound = objectMapper.readValue(message.getPayload(), ChatInbound.class);
        } catch (Exception e) {
            sendError(session, "消息格式错误");
            return;
        }
        if (inbound.getToUserId() == null || inbound.getToUserId().equals(fromId)) {
            sendError(session, "接收人不合法");
            return;
        }
        if (StrUtil.isBlank(inbound.getContent())) {
            sendError(session, "消息内容不能为空");
            return;
        }
        chatService.handleIncoming(fromId, inbound.getToUserId(), inbound.getContent().trim(), registry);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Long userId = currentUserId(session);
        if (userId != null) {
            registry.unregister(userId, session);
            log.info("WS 连接关闭 userId={}", userId);
        }
    }

    private void sendError(WebSocketSession session, String msg) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("type", "error", "msg", msg));
        session.sendMessage(new TextMessage(json));
    }

    private Long currentUserId(WebSocketSession session) {
        Object uid = session.getAttributes().get(WebSocketConfig.ATTR_USER_ID);
        return uid instanceof Long id ? id : null;
    }
}
