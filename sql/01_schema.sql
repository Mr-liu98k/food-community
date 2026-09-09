-- =====================================================================
-- 美食社区（Food Community）数据库结构
-- MySQL 8.0+ | utf8mb4
-- 说明：
--   1. 全表采用【逻辑外键 + 索引】，不建物理 FOREIGN KEY 约束，
--      数据一致性由 Service 层保证（方便建库、删库、导入顺序无忧）。
--   2. 与主表强绑定的从表（食材/步骤）删除时由应用层级联清理。
--   3. 执行方式：mysql -uroot -p < 01_schema.sql（或 Navicat 直接运行）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `food_community`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `food_community`;

-- ---------------------------------------------------------------------
-- 1. 用户表：普通用户 / 管理员 / 探店达人（一张表 + role + is_expert）
--    设计决策：不拆 admin/user 两张表。
--    登录只用一个接口，前端按 role 分流到用户端或管理端。
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号（唯一）',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称 / 展示名',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `intro`       VARCHAR(255) DEFAULT NULL COMMENT '个人简介（作者主页展示）',
  `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0=普通用户 1=管理员',
  `is_expert`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否探店达人：0=否 1=是（仅对普通用户有意义）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT ='用户/管理员/探店达人';

-- ---------------------------------------------------------------------
-- 2. 菜谱分类
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值（小在前）',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB COMMENT ='菜谱分类';

-- ---------------------------------------------------------------------
-- 3. 菜谱笔记主表（从表：recipe_ingredient / recipe_step）
--    status 状态机：0=待审核 1=已通过 2=已拒绝（拒绝必须填 reject_reason）
--    用户修改后状态重置为 0，需要管理员重新审核。
--    views / likes_count 为冗余计数，避免每次 COUNT 全表。
-- ---------------------------------------------------------------------
CREATE TABLE `recipe` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者 ID',
  `category_id`   BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
  `title`         VARCHAR(100) NOT NULL COMMENT '菜谱标题',
  `cover`         VARCHAR(255) NOT NULL COMMENT '封面图 URL',
  `description`   TEXT COMMENT '简介 / 导语（支持 Markdown）',
  `views`         INT          NOT NULL DEFAULT 0 COMMENT '浏览量（详情页访问自增）',
  `likes_count`   INT          NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数）',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=已通过 2=已拒绝',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝理由（status=2 时必填）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user`     (`user_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status`   (`status`),
  KEY `idx_views`    (`views`)
) ENGINE = InnoDB COMMENT ='菜谱笔记（主表）';

-- ---------------------------------------------------------------------
-- 4. 食材清单从表：一条菜谱 N 行食材
-- ---------------------------------------------------------------------
CREATE TABLE `recipe_ingredient` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id` BIGINT UNSIGNED NOT NULL COMMENT '所属菜谱 ID',
  `name`      VARCHAR(50) NOT NULL COMMENT '食材名称，如：鸡蛋',
  `amount`    VARCHAR(50) NOT NULL COMMENT '用量，如：2 个',
  `sort`      INT         NOT NULL DEFAULT 0 COMMENT '排序值',
  PRIMARY KEY (`id`),
  KEY `idx_recipe` (`recipe_id`)
) ENGINE = InnoDB COMMENT ='菜谱食材清单（从表）';

-- ---------------------------------------------------------------------
-- 5. 制作步骤从表：一条菜谱 N 步
-- ---------------------------------------------------------------------
CREATE TABLE `recipe_step` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id` BIGINT UNSIGNED NOT NULL COMMENT '所属菜谱 ID',
  `content`   TEXT    NOT NULL COMMENT '步骤文字（支持 Markdown）',
  `sort`      INT     NOT NULL DEFAULT 0 COMMENT '步骤序号',
  PRIMARY KEY (`id`),
  KEY `idx_recipe` (`recipe_id`)
) ENGINE = InnoDB COMMENT ='菜谱制作步骤（从表）';

-- ---------------------------------------------------------------------
-- 6. 菜谱点赞：切换赞/取消赞，唯一键防重复
-- ---------------------------------------------------------------------
CREATE TABLE `recipe_like` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id`   BIGINT UNSIGNED NOT NULL COMMENT '菜谱 ID',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '点赞用户 ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_user` (`recipe_id`, `user_id`)
) ENGINE = InnoDB COMMENT ='菜谱点赞';

-- ---------------------------------------------------------------------
-- 7. 菜谱收藏
-- ---------------------------------------------------------------------
CREATE TABLE `favorite` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id`   BIGINT UNSIGNED NOT NULL COMMENT '菜谱 ID',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '收藏用户 ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_user` (`recipe_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='菜谱收藏';

-- ---------------------------------------------------------------------
-- 8. 评论（楼中楼 / 盖楼式）
--    无限层级实现：parent_id 指向父评论；root_id 指向根评论。
--    根评论：parent_id=0 且 root_id=0。
--    删除某条评论时，递归清理其下所有子孙评论。
--    reply_user_id：被回复的用户（为空表示回复楼主），用于展示"回复 @xxx"。
-- ---------------------------------------------------------------------
CREATE TABLE `comment` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属菜谱 ID',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '评论者 ID',
  `parent_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父评论 ID（0=根评论）',
  `root_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '根评论 ID（自己是根则=0）',
  `reply_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被回复的用户 ID（空=回复楼主）',
  `content`       VARCHAR(500) NOT NULL COMMENT '评论内容',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipe` (`recipe_id`),
  KEY `idx_root`   (`root_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB COMMENT ='评论（楼中楼，父级ID+根节点ID）';

-- ---------------------------------------------------------------------
-- 9. 探店信息（用户发布的探店笔记）
--    同样走审核流：0=待审核 1=已通过 2=已拒绝。
--    达人热度 = SUM(该达人审核通过探店 views) * 2。
-- ---------------------------------------------------------------------
CREATE TABLE `store_visit` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者 ID',
  `title`         VARCHAR(100) NOT NULL COMMENT '探店标题',
  `cover`         VARCHAR(255) DEFAULT NULL COMMENT '封面图 URL',
  `content`       TEXT    NOT NULL COMMENT '探店体验内容（支持 Markdown）',
  `address`       VARCHAR(255) DEFAULT NULL COMMENT '探店店铺地址',
  `views`         INT     NOT NULL DEFAULT 0 COMMENT '浏览量（查看详情自增）',
  `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=已通过 2=已拒绝',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝理由',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user`   (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_views`  (`views`)
) ENGINE = InnoDB COMMENT ='探店信息';

-- ---------------------------------------------------------------------
-- 10. 探店预约（双主体审核）
--    同一条记录两种视角：
--      initiator_id = 发起预约的用户（我发起的）
--      expert_id    = 被预约的探店达人（我接收的）
--    status 状态机：0=待处理 1=已同意 2=已拒绝（拒绝必须填 reject_reason）
--    管理员只看不改；同意/拒绝由被预约达人操作。
-- ---------------------------------------------------------------------
CREATE TABLE `reservation` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `initiator_id`  BIGINT UNSIGNED NOT NULL COMMENT '发起人（预约方）用户 ID',
  `expert_id`     BIGINT UNSIGNED NOT NULL COMMENT '被预约探店达人用户 ID',
  `store_name`    VARCHAR(100) NOT NULL COMMENT '店铺名称',
  `store_intro`   VARCHAR(255) DEFAULT NULL COMMENT '店铺介绍',
  `store_address` VARCHAR(255) DEFAULT NULL COMMENT '店铺地址',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待处理 1=已同意 2=已拒绝',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝理由（status=2 必填）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_initiator` (`initiator_id`),
  KEY `idx_expert`    (`expert_id`),
  KEY `idx_status`    (`status`)
) ENGINE = InnoDB COMMENT ='探店预约';

-- ---------------------------------------------------------------------
-- 11. 轮播图：关联菜谱（创建时后端校验：所关联菜谱必须审核通过）
-- ---------------------------------------------------------------------
CREATE TABLE `banner` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联的菜谱 ID',
  `image`       VARCHAR(255) NOT NULL COMMENT '轮播图 URL',
  `sort`        INT      NOT NULL DEFAULT 0 COMMENT '排序值（小在前）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipe` (`recipe_id`)
) ENGINE = InnoDB COMMENT ='首页轮播图';

-- ---------------------------------------------------------------------
-- 12. 系统公告：管理端新增即对用户端可见（不设上下线开关，删除即下线）
-- ---------------------------------------------------------------------
CREATE TABLE `notice` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content`     TEXT         NOT NULL COMMENT '公告内容（支持 Markdown）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='系统公告';

-- ---------------------------------------------------------------------
-- 13. 聊天消息（一对一，WebSocket 实时推送 + 落库）
--    会话列表 = 按"对方用户"聚合出最近一条消息；未读数 = is_read=0 的条数。
--    不建独立会话表：一张消息表 + SQL 聚合即可。
-- ---------------------------------------------------------------------
CREATE TABLE `chat_message` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送方用户 ID',
  `to_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '接收方用户 ID',
  `content`      VARCHAR(1000) NOT NULL COMMENT '消息内容',
  `is_read`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读 1=已读',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_to_unread` (`to_user_id`, `is_read`),
  KEY `idx_from_to`   (`from_user_id`, `to_user_id`, `create_time`)
) ENGINE = InnoDB COMMENT ='一对一聊天消息';

-- =====================================================================
-- 附表结构完毕。共 13 张业务表：
--   user, category, recipe, recipe_ingredient, recipe_step,
--   recipe_like, favorite, comment, store_visit, reservation,
--   banner, notice, chat_message
-- 管理员/达人种子账号不在此文件，由后端启动时按配置初始化（见 README）。
-- =====================================================================
