package com.foodcommunity.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（普通用户 / 管理员 / 探店达人 三合一）。
 * password 加 @JsonProperty(WRITE_ONLY)：可反序列化、永不出现在任何响应 JSON 中。
 */
@Data
public class User {

    private Long id;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String nickname;

    private String avatar;

    private String intro;

    /** 角色：0=普通用户 1=管理员 */
    private Integer role;

    /** 是否探店达人（仅普通用户有意义）：0=否 1=是 */
    private Boolean isExpert;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
