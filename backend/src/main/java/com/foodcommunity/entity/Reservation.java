package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 探店预约（双主体）：
 * initiatorId = 发起预约的人；expertId = 被预约的探店达人。
 * 状态：0=待处理 1=已同意 2=已拒绝。审核权归被预约达人。
 * 联表字段：双方昵称与头像。
 */
@Data
public class Reservation {

    private Long id;

    private Long initiatorId;

    private Long expertId;

    private String storeName;

    private String storeIntro;

    private String storeAddress;

    private String phone;

    private String remark;

    /** 状态：0=待处理 1=已同意 2=已拒绝 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 联表字段 ----
    private String initiatorNickname;

    private String initiatorAvatar;

    private String expertNickname;

    private String expertAvatar;
}
