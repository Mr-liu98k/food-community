package com.foodcommunity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 美食专家对话请求。
 * 多轮实现：前端维护 messages（最近 10 轮）随请求发送，后端无状态拼装 system + history。
 */
@Data
public class AiChatRequest {

    @Valid
    @NotEmpty(message = "消息不能为空")
    @Size(max = 20, message = "消息条数过多")
    private List<AiMessage> messages = new ArrayList<>();

    @Data
    public static class AiMessage {

        /** 仅允许 user / assistant（system 由服务端注入） */
        @NotNull
        @Pattern(regexp = "user|assistant", message = "非法消息角色")
        private String role;

        @NotNull(message = "消息内容不能为空")
        @Size(max = 2000, message = "单条消息过长")
        private String content;
    }
}
