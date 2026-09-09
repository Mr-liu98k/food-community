-- =====================================================================
-- 美食社区 · M3 演示种子数据（单次执行）
-- demo_chef 升级为探店达人；补探店与预约样例，便于验收达人榜与预约双主体。
-- 执行：mysql -uroot -p < sql/03_seed_m3.sql
-- =====================================================================
USE food_community;

-- 1. demo_chef 设为探店达人（管理员视角的"设为达人"效果）
UPDATE `user` SET is_expert = 1 WHERE username = 'demo_chef';

SET @u = (SELECT id FROM `user` WHERE username = 'demo_chef');

-- 2. 探店信息（2 条通过 + 1 条待审）
INSERT INTO store_visit (user_id, title, cover, content, address, views, status, reject_reason) VALUES
(@u, '巷口那家开了二十年的牛肉面', '', '面条是老板凌晨现拉的，牛肉给得豪爽。**招牌红烧牛肉面** 18 元一碗，汤头清亮不腻。\n\n- 推荐：加一份卤蛋，配辣油绝了\n- 人均：20 元\n- 小贴士：中午 12 点前去不用排队', '幸福巷 12 号', 326, 1, NULL),
(@u, '藏在写字楼里的日式咖喱屋', '', '小店只有 8 个座位，咖喱是每天现熬三小时的。**炸猪排咖喱** 米饭可以免费续，配菜清爽解腻。\n\n装修是深夜食堂风，适合一个人安静吃饭。', '软件园二期 3 栋 1 层', 208, 1, NULL),
(@u, '新开的露营风烧烤店', '', '还没去过，先占个坑，等探店后再补全内容。', '滨江公园北门', 12, 0, NULL);

-- 3. 预约记录：一条发起的（给达人自己发的不太合理，改演示：发起人用 admin 以外？admin 是管理员。
--    为演示"我接收的"视图，此条 initiator 用一个演示普通用户。若无则建一个不可登录展示用户。
INSERT INTO `user` (username, password, nickname, avatar, intro, role, is_expert)
SELECT 'demo_fan', 'DEMO-NOT-LOGIN', '爱吃的小雨', '', '探店爱好者，记录每一家想去的店。', 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'demo_fan');

SET @f = (SELECT id FROM `user` WHERE username = 'demo_fan');

INSERT INTO reservation (initiator_id, expert_id, store_name, store_intro, store_address, phone, remark, status, reject_reason)
VALUES (@f, @u, '橘町日料', '新开的omakase，想去看看值不值得', '中山路 88 号', '13800000000', '希望周六中午时段', 0, NULL),
       (@f, @u, '山野咖啡', '手冲很出名，想约一期探店', '梧桐街 5 号', '13800000000', '', 1, NULL);
