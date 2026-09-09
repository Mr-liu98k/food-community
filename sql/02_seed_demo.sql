-- =====================================================================
-- 美食社区 · M1 演示种子数据（可选执行）
-- 说明：
--   * demo_chef 为展示作者，密码为占位符【不可登录】，实际登录请在前端注册；
--   * 菜谱覆盖三种审核状态，便于验证管理端筛选与前台可见性；
--   * 封面 cover 为空串 → 前端显示 CSS 渐变占位图（真实项目用本地上传）。
-- 执行：mysql -uroot -p < sql/02_seed_demo.sql
-- =====================================================================
USE food_community;

-- 1. 分类
INSERT INTO category (name, sort) VALUES
('家常菜', 1), ('烘焙甜品', 2), ('元气早餐', 3), ('靓汤羹', 4), ('凉菜', 5), ('小吃夜宵', 6)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. 演示作者（不可登录）
INSERT INTO `user` (username, password, nickname, avatar, intro, role, is_expert)
VALUES ('demo_chef', 'DEMO-NOT-LOGIN', '厨艺小当家', '', '爱下厨的程序员，记录每一道有故事的家常菜。', 0, 0)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

SET @u = (SELECT id FROM `user` WHERE username = 'demo_chef');

-- 3. 菜谱主表（2 通过 + 1 待审 + 1 拒绝）
INSERT INTO recipe (user_id, category_id, title, cover, description, views, likes_count, status, reject_reason) VALUES
(@u, (SELECT id FROM category WHERE name = '家常菜'), '可乐鸡翅', '', '零失败的家常硬菜，甜咸适口，宴客也拿得出手。', 268, 12, 1, NULL),
(@u, (SELECT id FROM category WHERE name = '烘焙甜品'), '免烤奥利奥芝士蛋糕', '', '不需要烤箱的懒人甜品，冰箱一晚就好，口感绵密。', 189, 9, 1, NULL),
(@u, (SELECT id FROM category WHERE name = '靓汤羹'), '西红柿炖牛腩', '', '汤浓肉烂，番茄的酸甜全炖进牛肉里。', 45, 3, 0, NULL),
(@u, (SELECT id FROM category WHERE name = '小吃夜宵'), '深夜炸鸡', '', '', 12, 1, 2, '内容过于简略，缺少关键步骤描述，请补充完整后重新提交');

SET @r1 = (SELECT id FROM recipe WHERE title = '可乐鸡翅');
INSERT INTO recipe_ingredient (recipe_id, name, amount, sort) VALUES
(@r1, '鸡翅中', '8 个', 0), (@r1, '可乐', '1 罐 (330ml)', 1), (@r1, '生抽', '2 勺', 2), (@r1, '姜片', '3 片', 3), (@r1, '料酒', '1 勺', 4);
INSERT INTO recipe_step (recipe_id, content, sort) VALUES
(@r1, '鸡翅两面各划一刀，冷水下锅加料酒焯水 2 分钟，捞出沥干。', 0),
(@r1, '平底锅少油，鸡翅中小火煎至两面金黄，下姜片炒香。', 1),
(@r1, '倒入可乐没过鸡翅，加生抽，大火烧开后转中小火焖 15 分钟。', 2),
(@r1, '开盖转大火收汁至浓稠冒泡，撒白芝麻即可。', 3);

SET @r2 = (SELECT id FROM recipe WHERE title = '免烤奥利奥芝士蛋糕');
INSERT INTO recipe_ingredient (recipe_id, name, amount, sort) VALUES
(@r2, '奶油奶酪', '200g', 0), (@r2, '淡奶油', '200ml', 1), (@r2, '奥利奥饼干', '1 包', 2), (@r2, '黄油', '30g', 3), (@r2, '细砂糖', '40g', 4);
INSERT INTO recipe_step (recipe_id, content, sort) VALUES
(@r2, '奥利奥去夹心擀碎，拌入融化黄油压实成饼底，冷藏 20 分钟。', 0),
(@r2, '奶油奶酪室温软化加糖打顺滑，淡奶油打至 6 分发后混合。', 1),
(@r2, '加入掰碎的奥利奥拌匀，倒入模具抹平表面。', 2),
(@r2, '冷藏 4 小时以上（隔夜最佳），脱模后表面撒饼干碎装饰。', 3);

SET @r3 = (SELECT id FROM recipe WHERE title = '西红柿炖牛腩');
INSERT INTO recipe_ingredient (recipe_id, name, amount, sort) VALUES
(@r3, '牛腩', '500g', 0), (@r3, '西红柿', '3 个', 1), (@r3, '洋葱', '半个', 2), (@r3, '番茄酱', '1 勺', 3), (@r3, '八角、桂皮', '少许', 4);
INSERT INTO recipe_step (recipe_id, content, sort) VALUES
(@r3, '牛腩切块冷水下锅焯水，去浮沫后捞出。', 0),
(@r3, '两个西红柿切块炒出沙，加番茄酱提色，放入牛腩翻炒。', 1),
(@r3, '加热水没过食材，放洋葱和香料，小火炖 90 分钟。', 2),
(@r3, '最后放剩下一个西红柿块再煮 10 分钟，加盐调味。', 3);
