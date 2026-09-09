package com.foodcommunity.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 简易 .env 加载器（约 30 行，不引入第三方 dotenv 依赖）。
 *
 * 规则：
 *  1. 依次探测 ./backend/.env 与 ../.env（覆盖 IDEA/命令行不同启动目录）；
 *  2. 只解析 KEY=VALUE 行，跳过空行与 # 注释；
 *  3. 仅当【系统环境变量】与【系统属性】都不存在该 key 时才注入，
 *     保证用户在操作系统/IDE 里显式配置的变量优先级更高。
 *
 * 必须在 SpringApplication.run() 之前调用，否则 yml 占位符解析不到。
 */
public final class EnvLoader {

    private EnvLoader() {
    }

    public static void load() {
        List<Path> candidates = List.of(
                Paths.get(".env").toAbsolutePath(),
                Paths.get("..", ".env").toAbsolutePath().normalize()
        );
        for (Path path : candidates) {
            if (Files.exists(path)) {
                loadFile(path);
                return;
            }
        }
        System.err.println("[EnvLoader] 未找到 .env 文件，将使用 application.yml 中的默认占位值。");
    }

    private static void loadFile(Path path) {
        try {
            for (String raw : Files.readAllLines(path)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }
                // 系统环境变量 > 已注入的系统属性 > .env
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
            System.out.println("[EnvLoader] 已加载环境配置: " + path);
        } catch (Exception e) {
            System.err.println("[EnvLoader] 读取 .env 失败: " + e.getMessage());
        }
    }
}
