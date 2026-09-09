package com.foodcommunity.mapper;

import com.foodcommunity.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 探店预约 Mapper。SQL 见 ReservationMapper.xml。
 */
public interface ReservationMapper {

    int insert(Reservation reservation);

    /** 我的预约：direction=initiated 查发起人 / received 查被预约达人 */
    List<Reservation> selectMyList(@Param("initiatorId") Long initiatorId,
                                   @Param("expertId") Long expertId);

    Reservation selectDetail(@Param("id") Long id);

    /** 管理端分页：keyword 匹配店铺名/电话/发起人昵称 */
    List<Reservation> selectAdminPage(@Param("keyword") String keyword);

    int updateStatus(@Param("id") Long id, @Param("status") int status, @Param("reason") String reason);

    int deleteById(@Param("id") Long id);

    /** 用户作为发起方或达人的预约数（删除用户前引用检查） */
    long countByParticipant(@Param("userId") Long userId);
}
