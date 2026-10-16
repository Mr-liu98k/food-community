-- =====================================================================
-- 美食社区 · 补充演示数据（首页轮播 + 系统公告）
-- 背景：02/03/04 号种子脚本覆盖了分类/菜谱/评论/探店/预约，
--       但 banner（首页轮播）与 notice（系统公告）两张表始终为空，
--       导致首页轮播区与管理端"公告管理"页面没有数据可展示。
-- 本脚本补齐这两块，并顺带点亮几个"统计数据"看板需要的样本
--       （点赞 / 收藏 / 聊天），让 ECharts 曲线与计数都有起伏。
-- 特性：可重复执行（全部使用 WHERE NOT EXISTS 幂等写法）。
-- 执行：mysql -uroot -p < sql/05_seed_supplement.sql
-- =====================================================================
USE food_community;

-- ---------------------------------------------------------------------
-- 1. 首页轮播图：关联审核通过的菜谱（后端建 banner 时要求菜谱 status=1）
--    注意：banner 无 recipe 外键约束，但前端会 JOIN recipe 取标题，
--    所以只挂 status=1 的菜谱，否则前端轮播查不出来。
-- ---------------------------------------------------------------------
INSERT INTO banner (recipe_id, image, sort)
SELECT r.id, '', 1 FROM recipe r
WHERE r.title = '可乐鸡翅' AND r.status = 1
  AND NOT EXISTS (SELECT 1 FROM banner b WHERE b.recipe_id = r.id);

INSERT INTO banner (recipe_id, image, sort)
SELECT r.id, '', 2 FROM recipe r
WHERE r.title = '免烤奥利奥芝士蛋糕' AND r.status = 1
  AND NOT EXISTS (SELECT 1 FROM banner b WHERE b.recipe_id = r.id);

INSERT INTO banner (recipe_id, image, sort)
SELECT r.id, '', 3 FROM recipe r
WHERE r.title = '西红柿炖牛腩' AND r.status = 1
  AND NOT EXISTS (SELECT 1 FROM banner b WHERE b.recipe_id = r.id);

-- ---------------------------------------------------------------------
-- 2. 系统公告（管理端新增即用户端可见，删除即下线）
-- ---------------------------------------------------------------------
INSERT INTO notice (title, content)
SELECT '欢迎来到美食社区 🍳', '这里是美食社区（Food Community）——一个可以分享家常菜谱、记录探店体验、向 AI 美食专家提问的地方。

### 你能做什么
- **发菜谱**：上传图文步骤，管理员审核通过后全站可见
- **写探店**：达人可发布探店笔记，还能被其他用户预约探店
- **找灵感**：首页轮播与分类浏览，收藏点赞随手记
- **问 AI**：AI 美食专家支持流式对话，边问边看

发布内容前请阅读社区规范，祝愿每一道菜都有故事。'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '欢迎来到美食社区 🍳');

INSERT INTO notice (title, content)
SELECT '社区内容规范（发帖前必读）', '为了让大家都能愉快地交流，请遵守以下规范：

1. **原创优先**：转载菜谱请注明来源，禁止直接搬运他人图文
2. **配图清晰**：封面图建议使用真实成品照，不要用无关图片
3. **步骤完整**：制作步骤请写清楚用量与火候，方便他人复现
4. **禁止广告**：不发布任何形式的商业推广、外链导流
5. **友善交流**：评论请就事论事，禁止人身攻击

违规内容管理员将直接拒绝或删除，情节严重的会限制账号功能。'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '社区内容规范（发帖前必读）');

INSERT INTO notice (title, content)
SELECT 'AI 美食专家上线啦', 'AI 美食专家已接入 **阿里云百炼（DashScope）** 大模型，支持 SSE 流式输出，边生成边显示。

### 你可以这样问
- 冰箱里有鸡胸肉和西兰花，能做什么快手菜？
- 红烧肉怎么才能做到肥而不腻？
- 新手烤箱第一次用，有什么要注意的？

> 提示：AI 的回答仅供参考，涉及食材过敏、特殊饮食禁忌请以专业意见为准。'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = 'AI 美食专家上线啦');

-- ---------------------------------------------------------------------
-- 3. 点亮看板数据：点赞 / 收藏
--    recipe_like 与 favorite 都有 (recipe_id, user_id) 唯一键，天然防重
-- ---------------------------------------------------------------------
SET @chef = (SELECT id FROM `user` WHERE username = 'demo_chef' LIMIT 1);
SET @fan  = (SELECT id FROM `user` WHERE username = 'demo_fan'  LIMIT 1);

-- 小雨 收藏了两道通过审核的菜谱
INSERT IGNORE INTO favorite (recipe_id, user_id)
SELECT r.id, @fan FROM recipe r
WHERE r.title IN ('可乐鸡翅', '免烤奥利奥芝士蛋糕') AND r.status = 1 AND @fan IS NOT NULL;

-- demo_fan 给可乐鸡翅点了赞（likes_count 冗余计数同步 +1）
INSERT IGNORE INTO recipe_like (recipe_id, user_id)
SELECT r.id, @fan FROM recipe r
WHERE r.title = '可乐鸡翅' AND @fan IS NOT NULL;

-- 冗余计数与明细表对齐（避免看板数字与详情页对不上）
UPDATE recipe r
SET r.likes_count = (SELECT COUNT(*) FROM recipe_like rl WHERE rl.recipe_id = r.id)
WHERE r.id IN (SELECT DISTINCT recipe_id FROM recipe_like);

-- ---------------------------------------------------------------------
-- 4. 点亮聊天数据：demo_fan <-> demo_chef 一来一回
--    会话列表按"对方用户"聚合，至少需要一组真实对话才能验收入口
-- ---------------------------------------------------------------------
INSERT INTO chat_message (from_user_id, to_user_id, content, is_read, create_time)
SELECT @fan, @chef, '你好呀，看到你发的可乐鸡翅了，周末想试试！', 1, NOW() - INTERVAL 3 HOUR
WHERE @fan IS NOT NULL AND @chef IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE from_user_id = @fan AND to_user_id = @chef);

INSERT INTO chat_message (from_user_id, to_user_id, content, is_read, create_time)
SELECT @chef, @fan, '欢迎欢迎～关键是最后收汁那步，火别开太大，冒大泡就关火。', 0, NOW() - INTERVAL 2 HOUR
WHERE @fan IS NOT NULL AND @chef IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_message WHERE from_user_id = @chef AND to_user_id = @fan);

-- =====================================================================
-- 执行完成校验（可手动跑一遍看看数字）
--   SELECT (SELECT COUNT(*) FROM banner) AS banner_cnt,
--          (SELECT COUNT(*) FROM notice) AS notice_cnt,
--          (SELECT COUNT(*) FROM favorite) AS favorite_cnt,
--          (SELECT COUNT(*) FROM recipe_like) AS like_cnt,
--          (SELECT COUNT(*) FROM chat_message) AS chat_cnt;
-- =====================================================================
