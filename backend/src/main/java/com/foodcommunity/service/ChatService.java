package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.ChatConversation;
import com.foodcommunity.entity.ChatMessage;
import com.foodcommunity.entity.User;
import com.foodcommunity.mapper.ChatMessageMapper;
import com.foodcommunity.mapper.UserMapper;
import com.foodcommunity.ws.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聊天模块：消息落库 + WebSocket 实时推送 + 会话聚合 + 未读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * WS 入站消息：落库并尝试实时推送给对方。
     * 无论对方是否在线都落库（离线消息靠会话拉取 + 未读数补偿）。
     */
    public void handleIncoming(Long fromId, Long toId, String content, WsSessionRegistry registry) {
        ChatMessage msg = new ChatMessage();
        msg.setFromUserId(fromId);
        msg.setToUserId(toId);
        msg.setContent(content);
        msg.setIsRead(0);
        chatMessageMapper.insert(msg);

        try {
            String payload = objectMapper.writeValueAsString(Map.of("type", "chat", "message", msg));
            if (!registry.sendTo(toId, payload)) {
                log.info("对方离线，消息已落库: from={} to={}", fromId, toId);
            }
        } catch (Exception e) {
            log.error("推送消息失败", e);
        }
    }

    /** 会话列表：按对方聚合，含最近消息与未读数，按最近时间降序 */
    public List<ChatConversation> conversations(Long userId) {
        List<ChatMessage> lastMsgs = chatMessageMapper.selectLastOfPeers(userId);
        if (lastMsgs.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> row : chatMessageMapper.selectUnreadByPeer(userId)) {
            Object pid = row.get("peerId");
            Object cnt = row.get("cnt");
            if (pid != null && cnt != null) {
                unreadMap.put(Long.valueOf(pid.toString()), Integer.valueOf(cnt.toString()));
            }
        }

        Set<Long> peerIds = new HashSet<>();
        for (ChatMessage m : lastMsgs) {
            peerIds.add(m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId());
        }
        Map<Long, User> userMap = new HashMap<>();
        for (User u : userMapper.findByIds(new ArrayList<>(peerIds))) {
            userMap.put(u.getId(), u);
        }

        List<ChatConversation> result = new ArrayList<>();
        for (ChatMessage m : lastMsgs) {
            Long peerId = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            User peer = userMap.get(peerId);
            if (peer == null) {
                continue; // 对方已被删除
            }
            ChatConversation c = new ChatConversation();
            c.setPeerId(peerId);
            c.setPeerNickname(peer.getNickname());
            c.setPeerAvatar(peer.getAvatar());
            c.setLastContent(m.getContent());
            c.setLastTime(m.getCreateTime());
            c.setUnread(unreadMap.getOrDefault(peerId, 0));
            result.add(c);
        }
        return result;
    }

    /** 与某人的历史：倒序分页（新在前），前端反转展示 */
    public PageResult<ChatMessage> history(Long userId, Long peerId, int page, int size) {
        PageHelper.startPage(page, size);
        PageInfo<ChatMessage> pageInfo = new PageInfo<>(chatMessageMapper.selectHistory(userId, peerId));
        return PageResult.of(pageInfo);
    }

    public int unreadCount(Long userId) {
        return chatMessageMapper.countUnread(userId);
    }

    public void markRead(Long userId, Long peerId) {
        if (peerId == null) {
            throw new BusinessException("参数错误");
        }
        chatMessageMapper.markRead(userId, peerId);
    }
}
