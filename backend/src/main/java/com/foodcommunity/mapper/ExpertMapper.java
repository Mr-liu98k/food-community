package com.foodcommunity.mapper;

import com.foodcommunity.dto.ExpertVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 达人榜 Mapper（在 UserMapper.xml 内实现 SQL）。
 */
public interface ExpertMapper {

    /**
     * 达人榜：role=0 且 is_expert=1 的用户，
     * heat = SUM(审核通过探店 views) * 2，无探店则 0，按热度降序。
     */
    List<ExpertVO> selectRank(@Param("limit") Integer limit);

    /** 校验某用户是否为探店达人 */
    int countExpert(@Param("id") Long id);
}
