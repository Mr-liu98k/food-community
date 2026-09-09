package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 探店信息（用户发布的探店笔记，走审核流）。
 * authorNickname / authorAvatar 为联表展示字段。
 */
@Data
public class StoreVisit {

    private Long id;

    private Long userId;

    private String title;

    /** 封面图 URL（空串显示占位） */
    private String cover;

    /** 探店体验内容（支持 Markdown） */
    private String content;

    /** 探店店铺地址 */
    private String address;

    private Integer views;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 联表字段 ----
    private String authorNickname;

    private String authorAvatar;
}
