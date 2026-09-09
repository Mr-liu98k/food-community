package com.foodcommunity.dto;

import lombok.Data;

/**
 * 探店列表查询参数（公开/我的/管理端/作者主页共用）。
 */
@Data
public class StoreVisitQuery {

    private String keyword;

    /** 审核状态：0/1/2，null 不限 */
    private Integer status;

    /** 作者 ID，null 不限 */
    private Long userId;
}
