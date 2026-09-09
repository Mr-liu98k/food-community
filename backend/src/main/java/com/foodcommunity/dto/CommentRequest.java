package com.foodcommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求。
 * parentId 为空 = 根评论；非空 = 回复某条评论（rootId / replyUserId 由服务端推导，防止篡改）。
 */
@Data
public class CommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长 500 字")
    private String content;

    /** 被回复的评论 ID（空=根评论） */
    private Long parentId;
}
