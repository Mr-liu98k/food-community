package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一对一聊天消息。
 */
@Data
public class ChatMessage {

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    /** 0=未读 1=已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
