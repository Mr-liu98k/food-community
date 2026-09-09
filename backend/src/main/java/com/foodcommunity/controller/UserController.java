package com.foodcommunity.controller;

import com.foodcommunity.common.Result;
import com.foodcommunity.dto.ChangePasswordRequest;
import com.foodcommunity.dto.UpdateProfileRequest;
import com.foodcommunity.entity.User;
import com.foodcommunity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户接口：个人中心（用户端与管理端共用同一接口，按登录身份取当前用户）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(userService.me());
    }

    @PutMapping("/me")
    public Result<User> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return Result.ok(userService.updateProfile(req));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(req);
        return Result.ok();
    }
}
