package com.foodcommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * WebSocket 聊天入站消息：{ toUserId, content }。
 */
@Data
public class ChatInbound {

    @NotNull(message = "接收人不能为空")
    private Long toUserId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息最长 1000 字")
    private String content;
}
