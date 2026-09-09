package com.foodcommunity.mapper;

import com.foodcommunity.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 Mapper。SQL 见 resources/mapper/UserMapper.xml。
 */
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    /** 批量按 ID 查（聊天会话列表用） */
    List<User> findByIds(@Param("ids") List<Long> ids);

    /** 管理端按角色分页（role + 昵称/账号模糊） */
    List<User> selectByRolePage(@Param("role") Integer role, @Param("keyword") String keyword);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    int updateExpert(@Param("id") Long id, @Param("expert") boolean expert);

    int deleteById(@Param("id") Long id);

    int insert(User user);

    /** 更新昵称/头像/简介（不含密码与账号） */
    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    long countByRole(@Param("role") Integer role);
}
