package com.foodcommunity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端审核请求：status=1 通过 / 2 拒绝。
 * 拒绝理由（reason）仅在 status=2 时必填，由 Service 层校验。
 */
@Data
public class AuditRequest {

    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果只能为通过(1)或拒绝(2)")
    @Max(value = 2, message = "审核结果只能为通过(1)或拒绝(2)")
    private Integer status;

    /** 拒绝理由（status=2 时必填） */
    private String reason;
}
