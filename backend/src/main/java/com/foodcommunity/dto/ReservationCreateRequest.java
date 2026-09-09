package com.foodcommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发起探店预约请求（预约的对象是被预约的探店达人）。
 */
@Data
public class ReservationCreateRequest {

    /** 被预约的探店达人用户 ID */
    @NotNull(message = "请选择要预约的探店达人")
    private Long expertId;

    @NotBlank(message = "请填写店铺名称")
    @Size(max = 100, message = "店铺名称最长 100 字")
    private String storeName;

    @Size(max = 255, message = "店铺介绍最长 255 字")
    private String storeIntro;

    @Size(max = 255, message = "店铺地址最长 255 字")
    private String storeAddress;

    @Size(max = 20, message = "联系电话最长 20 字")
    private String phone;

    @Size(max = 500, message = "备注最长 500 字")
    private String remark;
}
