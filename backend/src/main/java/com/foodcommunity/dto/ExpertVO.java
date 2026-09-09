package com.foodcommunity.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 探店达人榜条目。
 * heat = SUM(该达人审核通过探店 views) * 2，SQL 内直接算好。
 */
@Data
public class ExpertVO {

    private Long id;

    private String nickname;

    private String avatar;

    private String intro;

    /** 热度（views*2 合计） */
    private Integer heat;

    private LocalDateTime createTime;
}
