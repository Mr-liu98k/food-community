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
├─ sql/03_seed_m3.sql       # M3 探店/预约演示数据（可选执行）
├─ sql/04_seed_m6.sql       # M6 看板演示数据（可选执行）
├─ sql/05_seed_supplement.sql # 首页轮播 + 公告 + 点赞/收藏/聊天（可选，可重复执行）
├─ scripts/verify-m1.sh     # M1 验收脚本（本地运行）
├─ scripts/verify-m2.sh     # M2 验收脚本（本地运行）
├─ scripts/verify-m3.sh     # M3 验收脚本（本地运行）
├─ scripts/verify-audit.py  # ★ 内容审核链路自动化测试（58 项，改完审核代码跑这个）
├─ scripts/verify-audit.bat # ★ 上面的 Windows 一键入口（双击即可）
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
│  ├─ 11-需求规格说明书.md  # 角色权限 / 功能需求 / 业务流程 / 非功能需求
│  ├─ 12-测试用例.md        # 78 条测试用例 + 验收基线 + 缺陷记录
│  ├─ 13-配置参数说明.md    # 环境变量 / 端口 / AI / 数据库 / 排错速查
│  └─ 14-简历经历.md        # 项目经历（4 个版本）+ 面试追问预案
├─ backend/                 # Spring Boot 工程（全部里程碑完成）
├─ frontend/                # Vue3 工程（全部里程碑完成）
└─ .env.example             # MySQL / JWT 密钥 / 百炼 Key / 上传目录（模板）
```

## 文档索引

| 文档 | 用途 | 适合谁看 |
|---|---|---|
| `docs/11-需求规格说明书.md` | 讲清楚**要做什么**：角色权限矩阵、功能需求编号（FR-*）、审核状态机、预约双主体流程、非功能需求 | 需求评审、答辩讲解 |
| `docs/12-测试用例.md` | 讲清楚**怎么验**：78 条用例（编号/步骤/预期/实测）、验收基线数据、3 个已修复缺陷的分析 | 测试执行、质量说明 |
| `docs/13-配置参数说明.md` | 讲清楚**怎么配**：`.env` 全量参数、端口被占用的排查、AI 接入、12 条常见问题速查 | 环境搭建、部署运维 |
| `docs/14-简历经历.md` | 讲清楚**我做了什么**：4 个版本（标准/精简/测试岗定向/STAR 口述）+ 主动排查的 3 个生产级缺陷 + 面试追问预案 | 求职简历、面试自述 |

## 设计文档阅读顺序

1. `docs/01-数据库设计.md` —— 先看 §1 表清单 + §6 精简决策（为什么只有 13 张表）
2. `docs/02-接口清单.md` —— 按模块看接口，§0 先看拦截规则
3. `docs/03-开发排期.md` —— 里程碑顺序与每阶段验收清单

## 前置环境

JDK 17+ / Maven 3.8+ / Node.js 18+ / MySQL 8.0+（建库执行 `sql/01_schema.sql`）。
管理员种子账号**不入 SQL**：后端启动时按 `.env` 中 `ADMIN_USERNAME / ADMIN_PASSWORD` 自动创建。

## 本地启动（三步）

**1. 数据库**（建库建表 + 演示数据，按顺序执行）

```bash
mysql -uroot -p < sql/01_schema.sql          # 13 张表
mysql -uroot -p < sql/02_seed_demo.sql       # 分类 + 菜谱 + 作者
mysql -uroot -p < sql/03_seed_m3.sql         # 探店 + 预约
mysql -uroot -p < sql/04_seed_m6.sql         # 看板时间分布 + 楼中楼评论
mysql -uroot -p < sql/05_seed_supplement.sql # 首页轮播 + 公告 + 点赞/收藏/聊天
```

> `05_seed_supplement.sql` 补齐了 02~04 未覆盖的 `banner`（首页轮播）与 `notice`（公告）
> 两张表，并给看板/聊天页留下样本数据，全部采用 `WHERE NOT EXISTS` 幂等写法，可反复执行。

**2. 后端**

```bash
cp backend/.env.example backend/.env   # 填入本机 MySQL 账号密码
cd backend && mvn clean package -DskipTests
java -jar target/food-community-backend-0.0.1-SNAPSHOT.jar
```

**端口统一由 `backend/.env` 的 `SERVER_PORT` 控制**（默认 8080）。
若本机 8080 被其他程序占用（很常见，比如已跑着别的项目），
**只改 `.env` 这一处即可**，不需要加 `--server.port` 参数：

```ini
# backend/.env
SERVER_PORT=8081
```

前端代理与 `scripts/` 下的全部验收脚本都会**自动读取这个值**，无需手工对齐。

**3. 前端**

```bash
cd frontend && npm install
npm run dev
```

前端跑在 <http://localhost:5173>，`/api` 与 `/uploads` 已配置代理到后端（端口自动跟随 `backend/.env`），无需处理跨域。
管理员默认账号见 `backend/.env`（`admin` / `admin123`）。

> 临时覆盖端口（不改 `.env`）：`BACKEND_PORT=8081 npm run dev` 或
> `BACKEND_PORT=8081 bash scripts/verify-m1.sh`

## 内容审核自动化测试

改完审核相关代码后，跑一条命令回归「菜谱 + 探店」双线审核全链路：

```bash
python scripts/verify-audit.py
```

端口会自动从 `backend/.env` 读取，**正常情况无需任何参数**。
Windows 下也可直接双击 `scripts\verify-audit.bat`。

Windows 下也可以直接双击 `scripts\verify-audit.bat`。

**零依赖**：只用 Python 标准库，不需要 `pip install` 任何东西。

**覆盖范围**（58 项断言，约 0.7 秒跑完）：

| 分组 | 覆盖内容 |
|---|---|
| 待审隔离 | 提交后前台详情不可见、不进前台列表、我的可见、管理端 status=0 可查 |
| 拒绝分支 | 不填理由被拦、理由为空白被拦、正常拒绝成功、理由落库、被拒后前台不可见、改判被拦 |
| 通过分支 | 通过成功、status=1、拒绝理由清空、前台可见、进列表、浏览量 +1 |
| 重复审核 | 已通过/已拒后再次审核 → 「不在待审核状态」拦截 |
| 参数边界 | status=0/3/缺省 → 参数校验拦截（code=400）；不存在的 ID |
| 鉴权 | 未登录 → 401；普通用户调管理端 → 403 |
| 编辑重置 | 编辑后 status 归 0、拒绝理由清空、前台立即隐藏、浏览量保留 |
| 轮播联动 | 关联待审菜谱被拦、关联已通过菜谱成功 |

**数据策略**：测试数据统一带 `AUDIT-<时间戳>-` 前缀，与演示数据完全隔离；**全部通过则自动清理**，失败则**保留现场**并打印残留 ID 便于排查。

## 当前状态

🎉 **M0~M6 全部完成——规格书功能全量闭环**（双端 + 审核流 + 楼中楼 + 达人热度 + 预约双主体 + AI 流式 + WS 聊天 + ECharts 看板 + 用户管理）。
里程碑报告与全项目闭环核对见 `docs/04~10`。可直接进入验收演示与答辩准备。
