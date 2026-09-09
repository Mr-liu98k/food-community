package com.foodcommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 达人拒绝预约请求（拒绝理由必填）。
 */
@Data
public class RejectRequest {

    @NotBlank(message = "拒绝时必须填写拒绝理由")
    @Size(max = 255, message = "理由最长 255 字")
    private String reason;
}
