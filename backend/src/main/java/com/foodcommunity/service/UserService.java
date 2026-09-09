package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.AuthorProfile;
import com.foodcommunity.dto.ChangePasswordRequest;
import com.foodcommunity.dto.LoginRequest;
import com.foodcommunity.dto.LoginResponse;
import com.foodcommunity.dto.RegisterRequest;
import com.foodcommunity.dto.UpdateProfileRequest;
import com.foodcommunity.entity.User;
import com.foodcommunity.mapper.RecipeMapper;
import com.foodcommunity.mapper.StoreVisitMapper;
import com.foodcommunity.mapper.UserMapper;
import com.foodcommunity.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 账号模块：注册 / 登录 / 当前用户资料 / 修改资料 / 修改密码 / 启动初始化管理员。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RecipeMapper recipeMapper;
    private final StoreVisitMapper storeVisitMapper;

    public void register(RegisterRequest req) {
        String username = req.getUsername().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setNickname(StrUtil.isBlank(req.getNickname()) ? username : req.getNickname().trim());
        user.setRole(0);
        user.setIsExpert(false);
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.findByUsername(req.getUsername().trim());
        // 统一提示，避免暴露"用户名是否存在"
        if (user == null || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.createToken(user.getId(), user.getRole());
        return new LoginResponse(token, user);
    }

    /** 当前登录用户完整信息；未登录抛 401。 */
    public User me() {
        Long id = UserContext.id();
        if (id == null) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User updateProfile(UpdateProfileRequest req) {
        User user = me();
        user.setNickname(req.getNickname().trim());
        user.setAvatar(req.getAvatar());
        user.setIntro(req.getIntro());
        userMapper.updateProfile(user);
        return userMapper.findById(user.getId());
    }

    public void changePassword(ChangePasswordRequest req) {
        User user = me();
        if (!BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(user.getId(), BCrypt.hashpw(req.getNewPassword()));
    }

    /** 作者/达人主页信息：用户资料 + 已通过菜谱数 + 已通过探店数 */
    public AuthorProfile authorProfile(Long authorId) {
        User user = userMapper.findById(authorId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new AuthorProfile(user,
                recipeMapper.countApprovedByUser(authorId),
                storeVisitMapper.countApprovedByUser(authorId));
    }

    /**
     * 启动初始化：若系统内一个管理员都没有，则按 .env 配置创建一个。
     */
    public void createAdminIfAbsent(String username, String password) {
        if (userMapper.countByRole(1) > 0) {
            return;
        }
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalStateException(
                    "检测到不存在管理员，但 ADMIN_USERNAME[" + username + "]已被普通用户占用，请修改 backend/.env 后重启");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(BCrypt.hashpw(password));
        admin.setNickname("超级管理员");
        admin.setRole(1);
        admin.setIsExpert(false);
        userMapper.insert(admin);
        log.info("已自动创建管理员账号: {}", username);
    }
}
