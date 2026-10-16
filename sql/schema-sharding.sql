-- ============================================================
-- 分库分表示例: 订单表按 buyer_id 取模拆成 4 张物理表
-- 配合 ShardingSphere-JDBC 使用 (spring.profiles.active=sharding)
--
-- 执行方式:
--   mysql -u root -p campus_trading < sql/schema-sharding.sql
-- ============================================================

USE `campus_trading`;

-- ------------------------------------------------------------
-- 1. 由原表派生 4 张分表, 保证字段/索引/字符集与 orders 完全一致
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders_0` LIKE `orders`;
CREATE TABLE IF NOT EXISTS `orders_1` LIKE `orders`;
CREATE TABLE IF NOT EXISTS `orders_2` LIKE `orders`;
CREATE TABLE IF NOT EXISTS `orders_3` LIKE `orders`;

-- ------------------------------------------------------------
-- 2. 关键: 主键全局唯一
--    orders 表用的是 MySQL 自增主键, 拆表后每张表各自从 1 开始自增,
--    会出现 orders_0.id = 1 与 orders_1.id = 1 撞车, 按 id 查询时返回多行。
--    这里用「分段起点」解决: 每张表的自增起点错开 100 万, 简单且无需改代码。
--    生产环境数据量大时应改用雪花算法(但要注意前端 JS 对 19 位 Long 的精度丢失问题)。
-- ------------------------------------------------------------
ALTER TABLE `orders_0` AUTO_INCREMENT = 1;
ALTER TABLE `orders_1` AUTO_INCREMENT = 1000001;
ALTER TABLE `orders_2` AUTO_INCREMENT = 2000001;
ALTER TABLE `orders_3` AUTO_INCREMENT = 3000001;

-- ------------------------------------------------------------
-- 3. 迁移存量数据 (按同样的取模规则打散到分表)
--    用 INSERT IGNORE: 脚本可以重复执行, 已迁移过的行不会重复插入
--    注意: 显式插入 id 后, 自增起点会自动调整到 max(id) + 1,
--    所以第 2 步的 ALTER 要放在数据迁移之后、正式启用分片之前再执行一次。
-- ------------------------------------------------------------
INSERT IGNORE INTO `orders_0` SELECT * FROM `orders` WHERE MOD(`buyer_id`, 4) = 0;
INSERT IGNORE INTO `orders_1` SELECT * FROM `orders` WHERE MOD(`buyer_id`, 4) = 1;
INSERT IGNORE INTO `orders_2` SELECT * FROM `orders` WHERE MOD(`buyer_id`, 4) = 2;
INSERT IGNORE INTO `orders_3` SELECT * FROM `orders` WHERE MOD(`buyer_id`, 4) = 3;

-- 迁移完成后重新校准自增起点
ALTER TABLE `orders_0` AUTO_INCREMENT = 1;
ALTER TABLE `orders_1` AUTO_INCREMENT = 1000001;
ALTER TABLE `orders_2` AUTO_INCREMENT = 2000001;
ALTER TABLE `orders_3` AUTO_INCREMENT = 3000001;

-- ------------------------------------------------------------
-- 4. 校验: 分表总数应与原表一致
-- ------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM `orders`) AS 原表总数,
    (SELECT COUNT(*) FROM `orders_0`) + (SELECT COUNT(*) FROM `orders_1`)
  + (SELECT COUNT(*) FROM `orders_2`) + (SELECT COUNT(*) FROM `orders_3`) AS 分表总数;

-- 确认无误后可清理原表(建议先备份, 确认无误再执行):
-- RENAME TABLE `orders` TO `orders_backup`;
