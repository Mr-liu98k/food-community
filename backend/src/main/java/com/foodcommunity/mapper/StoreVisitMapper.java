package com.foodcommunity.mapper;

import com.foodcommunity.dto.StoreVisitQuery;
import com.foodcommunity.entity.StoreVisit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 探店信息 Mapper。
 */
public interface StoreVisitMapper {

    List<StoreVisit> selectByQuery(@Param("q") StoreVisitQuery query);

    /** 前台热门：已通过，views 降序 */
    List<StoreVisit> selectHot(@Param("limit") int limit);

    StoreVisit selectDetail(@Param("id") Long id);

    /** 该作者已通过探店数（作者主页） */
    long countApprovedByUser(@Param("userId") Long userId);

    /** 该作者全部探店数（删除用户前引用检查） */
    long countByUser(@Param("userId") Long userId);

    int insert(StoreVisit storeVisit);

    /** 编辑内容（status/rejectReason 由 Service 重置） */
    int updateContent(StoreVisit storeVisit);

    int updateViews(@Param("id") Long id);

    int updateAudit(@Param("id") Long id, @Param("status") int status, @Param("reason") String reason);

    int deleteById(@Param("id") Long id);
}
