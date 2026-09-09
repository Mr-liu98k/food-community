-- =====================================================================
-- 美食社区 · M6 看板演示数据（单次执行，重复执行会产生重复评论）
-- 1) 把演示菜谱发布时间分散到近 6 天 → 折线图有起伏
-- 2) 补一条根评论 + 楼中楼回复 → 评论计数 > 0
-- =====================================================================
USE food_community;

-- 1. 发布时间回填（标题定位，避免影响用户自建数据）
UPDATE recipe SET create_time = NOW() - INTERVAL 1 DAY   WHERE title = '免烤奥利奥芝士蛋糕';
UPDATE recipe SET create_time = NOW() - INTERVAL 2 DAY   WHERE title = '可乐鸡翅';
UPDATE recipe SET create_time = NOW() - INTERVAL 4 DAY   WHERE title = '西红柿炖牛腩';
UPDATE recipe SET create_time = NOW() - INTERVAL 6 DAY   WHERE title = '深夜炸鸡';

-- 2. 评论演示：小雨 评论可乐鸡翅，小当家 回复（楼中楼）
SET @r = (SELECT id FROM recipe WHERE title = '可乐鸡翅' LIMIT 1);
SET @fan = (SELECT id FROM `user` WHERE username = 'demo_fan');
SET @chef = (SELECT id FROM `user` WHERE username = 'demo_chef');

INSERT INTO `comment` (recipe_id, user_id, parent_id, root_id, reply_user_id, content, create_time)
VALUES (@r, @fan, 0, 0, NULL, '照着做了两次，孩子超爱吃！收汁那步太关键了。', NOW() - INTERVAL 1 DAY);

SET @root = LAST_INSERT_ID();
INSERT INTO `comment` (recipe_id, user_id, parent_id, root_id, reply_user_id, content, create_time)
VALUES (@r, @chef, @root, @root, @fan, '谢谢喜欢！可以试试加点话梅，酸甜层次更丰富～', NOW() - INTERVAL 20 HOUR);
