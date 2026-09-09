package com.foodcommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布/编辑探店信息请求。
 */
@Data
public class StoreVisitSaveRequest {

    @NotBlank(message = "探店标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    private String cover;

    @NotBlank(message = "探店内容不能为空")
    @Size(max = 10000, message = "探店内容过长")
    private String content;

    @Size(max = 255, message = "地址最长 255 字")
    private String address;
}
