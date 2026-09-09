package com.foodcommunity.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.List;

/**
 * 统一分页结构：{ records, total, current, size }。
 * 由 PageHelper 的 PageInfo 转换而来。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long current;
    private long size;

    public static <T> PageResult<T> of(PageInfo<T> pageInfo) {
        PageResult<T> result = new PageResult<>();
        result.records = pageInfo.getList();
        result.total = pageInfo.getTotal();
        result.current = pageInfo.getPageNum();
        result.size = pageInfo.getPageSize();
        return result;
    }
}
