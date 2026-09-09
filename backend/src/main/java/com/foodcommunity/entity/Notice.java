package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告。
 */
@Data
public class Notice {

    private Long id;

    private String title;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
