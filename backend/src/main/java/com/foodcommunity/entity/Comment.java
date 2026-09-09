package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论（楼中楼）。
 * 无限层级存储：parentId 指向父评论；rootId 指向根评论（根评论两者均为 0）。
 * authorNickname / authorAvatar / replyNickname / recipeTitle 为联表展示字段。
 * children 为组装字段：根评论分页时携带其全部子评论（按时间正序）。
 */
@Data
public class Comment {

    private Long id;

    private Long recipeId;

    private Long userId;

    /** 父评论 ID（0=根评论） */
    private Long parentId;

    /** 根评论 ID（自己就是根则=0） */
    private Long rootId;

    /** 被回复的用户 ID（空=回复楼主/根评论） */
    private Long replyUserId;

    private String content;

    private LocalDateTime createTime;

    // ---- 联表/组装字段 ----
    private String authorNickname;

    private String authorAvatar;

    private String replyNickname;

    private String recipeTitle;

    private List<Comment> children = new ArrayList<>();
}
