package com.foodcommunity.mapper;

import com.foodcommunity.entity.Notice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告 Mapper。
 */
public interface NoticeMapper {

    List<Notice> selectPage(@Param("keyword") String keyword);

    List<Notice> selectLatest(@Param("limit") int limit);

    Notice selectById(@Param("id") Long id);

    int insert(Notice notice);

    int update(Notice notice);

    int deleteById(@Param("id") Long id);
}
