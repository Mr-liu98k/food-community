# Food Community · 美食社区

前后端分离的美食分享社区：菜谱发布/审核、楼中楼评论、收藏点赞、探店达人 + 预约双主体审核、
AI 美食专家（SSE 流式）、WebSocket 一对一聊天、管理端 ECharts 数据看板。

## 技术栈（已与需求方确认的版本决策）

| 层 | 选型 | 决策说明 |
|---|---|---|
| 后端 | **Spring Boot 3.5.x** + SpringAI + MyBatis + PageHelper + Hutool + java-jwt + WebSocket + Validation | 需求稿写 SpringBoot4，经确认改用 3.5.x——MyBatis 生态与资料最稳，答辩省心，功能零损失 |
| AI | 阿里云百炼（DashScope），SpringAI 官方 dashscope 适配 | API Key 与模型名一律走 `.env` |
| 前端 | Vue3 + Vite + Element-Plus + Vue-Router + Pinia + Axios + ECharts + markdown-it | 单工程双布局（用户端 Layout + 管理端 Layout），按 role 路由守卫分流 |
| 数据库 | MySQL 8.0+（utf8mb4），13 张业务表 | 见 `sql/01_schema.sql` |
| 架构纪律 | Ponytail（最少可行）/ Hallmark（页面设计防模板感） | 精简决策见 `docs/01-数据库设计.md §6` |

## 目录结构

```
food-community/
├─ sql/01_schema.sql        # 建库建表（13 张表）
├─ sql/02_seed_demo.sql     # M1 演示数据（可选执行）
├─ scripts/verify-m1.sh     # M1 验收脚本（本地运行）
├─ scripts/verify-m2.sh     # M2 验收脚本（本地运行）
├─ scripts/verify-m3.sh     # M3 验收脚本（本地运行）
├─ docs/
│  ├─ 01-数据库设计.md      # 表关系 / 状态机 / 统计口径 / 精简决策
│  ├─ 02-接口清单.md        # REST 接口，模块化
│  ├─ 03-开发排期.md        # M0~M6 里程碑
│  ├─ 04-M0-完成报告.md     # M0 验收与运行指南
│  ├─ 05-M1-完成报告.md     # M1 验收（分类+菜谱审核主链）
│  ├─ 06-M2-完成报告.md     # M2 验收（评论楼中楼/点赞收藏/作者主页/轮播）
│  ├─ 07-M3-完成报告.md     # M3 验收（探店审核/达人榜热度/预约双主体）
│  ├─ 08-M4-完成报告.md     # M4 验收（AI 美食专家 SSE 流式）
│  ├─ 09-M5-完成报告.md     # M5 验收（WebSocket 聊天 + 公告）
│  └─ 10-M6-完成报告.md     # M6 验收（看板 + 用户管理）+ 全项目闭环总览
├─ backend/                 # Spring Boot 工程（全部里程碑完成）
├─ frontend/                # Vue3 工程（全部里程碑完成）
└─ .env.example             # MySQL / JWT 密钥 / 百炼 Key / 上传目录（模板）
```

## 设计文档阅读顺序

1. `docs/01-数据库设计.md` —— 先看 §1 表清单 + §6 精简决策（为什么只有 13 张表）
2. `docs/02-接口清单.md` —— 按模块看接口，§0 先看拦截规则
3. `docs/03-开发排期.md` —— 里程碑顺序与每阶段验收清单

## 前置环境

JDK 17+ / Maven 3.8+ / Node.js 18+ / MySQL 8.0+（建库执行 `sql/01_schema.sql`）。
管理员种子账号**不入 SQL**：后端启动时按 `.env` 中 `ADMIN_USERNAME / ADMIN_PASSWORD` 自动创建。

## 当前状态

🎉 **M0~M6 全部完成——规格书功能全量闭环**（双端 + 审核流 + 楼中楼 + 达人热度 + 预约双主体 + AI 流式 + WS 聊天 + ECharts 看板 + 用户管理）。
里程碑报告与全项目闭环核对见 `docs/04~10`。可直接进入验收演示与答辩准备。
