package com.foodcommunity.config;

import com.foodcommunity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动后初始化：系统内无管理员时，按 .env 的 ADMIN_USERNAME/ADMIN_PASSWORD 自动创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        try {
            userService.createAdminIfAbsent(adminUsername, adminPassword);
        } catch (Exception e) {
            // 初始化失败只告警不阻断启动，避免刚建库时因配置问题整个应用起不来
            log.error("管理员自动初始化失败（如需修复请检查 backend/.env 配置后重启）: {}", e.getMessage());
        }
    }
}
