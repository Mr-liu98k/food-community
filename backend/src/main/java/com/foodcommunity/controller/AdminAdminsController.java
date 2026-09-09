package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.AdminUserCreateRequest;
import com.foodcommunity.dto.ResetPasswordRequest;
import com.foodcommunity.entity.User;
import com.foodcommunity.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端管理员管理（仅管理员）：查/增/改/重置密码/删（禁删自己）。
 */
@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminAdminsController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        return Result.ok(userAdminService.page(1, page, size, keyword));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody AdminUserCreateRequest req) {
        userAdminService.create(1, req);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> updateNickname(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userAdminService.updateNickname(id, body.get("nickname"));
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userAdminService.resetPassword(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id, UserContext.id());
        return Result.ok();
    }
}
