package com.foodcommunity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置/取消探店达人请求。
 */
@Data
public class ExpertRequest {

    @NotNull(message = "expert 不能为空")
    private Boolean expert;
}
