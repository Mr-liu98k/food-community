package com.foodcommunity.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表条目（按"对方用户"聚合）。
 */
@Data
public class ChatConversation {

    /** 对方用户 ID */
    private Long peerId;

    private String peerNickname;

    private String peerAvatar;

    /** 该会话最近一条消息 */
    private String lastContent;

    private LocalDateTime lastTime;

    /** 未读数（对方发给我的 is_read=0 条数） */
    private Integer unread;
}
