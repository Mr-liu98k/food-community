package com.foodcommunity;

import com.foodcommunity.config.EnvLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 美食社区后端入口。
 * 启动顺序：先加载 .env 文件到系统属性，再启动 Spring 容器，
 * 这样 application.yml 里的 ${VARIABLE} 占位符才能读到 .env 中的值。
 */
@SpringBootApplication
@MapperScan("com.foodcommunity.mapper")
public class FoodCommunityApplication {

    public static void main(String[] args) {
        EnvLoader.load();
        SpringApplication.run(FoodCommunityApplication.class, args);
    }
}
