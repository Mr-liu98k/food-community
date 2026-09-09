package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.AdminUserCreateRequest;
import com.foodcommunity.dto.ResetPasswordRequest;
import com.foodcommunity.entity.User;
import com.foodcommunity.mapper.CommentMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.foodcommunity.mapper.ReservationMapper;
import com.foodcommunity.mapper.StoreVisitMapper;
import com.foodcommunity.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端 用户/管理员 管理：
 * - role=0 普通用户：查/增/改/重置密码/设置达人/删（有业务数据禁止删）
 * - role=1 管理员：查/增/改/重置密码/删（禁删自己，有业务数据禁止删）
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserMapper userMapper;
    private final RecipeMapper recipeMapper;
    private final StoreVisitMapper storeVisitMapper;
    private final CommentMapper commentMapper;
    private final ReservationMapper reservationMapper;

    public PageResult<User> page(int role, int page, int size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<User> pageInfo = new PageInfo<>(
                userMapper.selectByRolePage(role, StrUtil.blankToDefault(keyword, null)));
        return PageResult.of(pageInfo);
    }

    /** 新增（管理员或普通用户，由 role 决定） */
    public void create(int role, AdminUserCreateRequest req) {
        String username = req.getUsername().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setNickname(StrUtil.isBlank(req.getNickname()) ? username : req.getNickname().trim());
        user.setRole(role);
        user.setIsExpert(false);
        userMapper.insert(user);
    }

    public void updateNickname(Long id, String nickname) {
        requireUser(id);
        if (StrUtil.isBlank(nickname)) {
            throw new BusinessException("昵称不能为空");
        }
        userMapper.updateNickname(id, nickname.trim());
    }

    public void resetPassword(Long id, ResetPasswordRequest req) {
        requireUser(id);
        userMapper.updatePassword(id, BCrypt.hashpw(req.getPassword()));
    }

    /** 设置/取消探店达人（仅对普通用户生效） */
    public void setExpert(Long id, boolean expert) {
        User user = requireUser(id);
        if (user.getRole() != 0) {
            throw new BusinessException("仅普通用户可以设置为探店达人");
        }
        userMapper.updateExpert(id, expert);
    }

    /**
     * 删除用户。规则：
     * - 管理员不能删除自己；
     * - 存在菜谱/探店/评论/预约等业务数据的用户禁止删除（避免脏引用），提示先处理其内容。
     */
    public void delete(Long id, Long operatorId) {
        User user = requireUser(id);
        if (user.getRole() == 1 && user.getId().equals(operatorId)) {
            throw new BusinessException("不能删除当前登录的管理员自己");
        }
        long refs = recipeMapper.countByUser(id)
                + storeVisitMapper.countByUser(id)
                + commentMapper.countByUser(id)
                + reservationMapper.countByParticipant(id);
        if (refs > 0) {
            throw new BusinessException("该用户存在菜谱/探店/评论/预约等业务数据，请先处理后删除");
        }
        userMapper.deleteById(id);
    }

    private User requireUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
