package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.ChatConversation;
import com.foodcommunity.entity.ChatMessage;
import com.foodcommunity.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 聊天 REST（登录）：会话列表 / 历史 / 未读数 / 标记已读。
 * 实时收发走 WebSocket /api/ws/chat?token=xxx。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public Result<List<ChatConversation>> conversations() {
        return Result.ok(chatService.conversations(UserContext.id()));
    }

    @GetMapping("/history")
    public Result<PageResult<ChatMessage>> history(@RequestParam Long peerId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(chatService.history(UserContext.id(), peerId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        return Result.ok(chatService.unreadCount(UserContext.id()));
    }

    @PostMapping("/read")
    public Result<Void> markRead(@RequestBody Map<String, Long> body) {
        chatService.markRead(UserContext.id(), body.get("peerId"));
        return Result.ok();
    }
}
