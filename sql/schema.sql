-- ============================================================================
-- 校园二手物品交易平台 数据库设计脚本 (MySQL 8.x)
-- 字符集: utf8mb4 ; 引擎: InnoDB
-- 共 6 张业务表: user / category / item / chat_message / orders / sensitive_word
-- ============================================================================
CREATE DATABASE IF NOT EXISTS campus_trading DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_trading;

-- ----------------------------------------------------------------------------
-- 1. 用户表 user
--    角色: STUDENT-学生(买家/卖家) ADMIN-管理员
--    状态: 1-正常 0-禁用
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/ADMIN',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- 2. 分类表 category (支持两级: parent_id=0 为一级分类)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID, 0为一级',
  `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值, 越小越靠前',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品分类表';

-- ----------------------------------------------------------------------------
-- 3. 物品表 item
--    状态: 0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回
--    images: 多张图片URL, 逗号分隔
--    sensitive_hit: 发布时是否命中敏感词(1-是 0-否), 命中后进入审核并高亮提示管理员
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  `user_id`        BIGINT         NOT NULL COMMENT '发布人ID',
  `category_id`    BIGINT         NOT NULL COMMENT '分类ID',
  `title`          VARCHAR(100)   NOT NULL COMMENT '标题',
  `description`    TEXT           COMMENT '描述',
  `price`          DECIMAL(10,2)  NOT NULL COMMENT '价格(元)',
  `images`         VARCHAR(2000)  DEFAULT NULL COMMENT '图片URL,逗号分隔',
  `status`         TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1在售 2已下架 3已售出 4已驳回',
  `view_count`     INT            NOT NULL DEFAULT 0 COMMENT '浏览量',
  `sensitive_hit`  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否命中敏感词: 1-是 0-否',
  `reject_reason`  VARCHAR(255)   DEFAULT NULL COMMENT '驳回原因',
  `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手物品表';

-- ----------------------------------------------------------------------------
-- 4. 聊天消息表 chat_message (私信议价: 双方针对某件物品协商价格)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id` BIGINT       NOT NULL COMMENT '发送人ID',
  `to_user_id`   BIGINT       NOT NULL COMMENT '接收人ID',
  `item_id`      BIGINT       DEFAULT NULL COMMENT '关联物品ID(议价上下文)',
  `content`      VARCHAR(1000) NOT NULL COMMENT '消息内容',
  `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 1-是 0-否',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_to` (`from_user_id`, `to_user_id`),
  KEY `idx_to_read` (`to_user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ----------------------------------------------------------------------------
-- 5. 订单表 orders
--    状态: 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消
--    price: 下单时价格, 支持按聊天中议价后的金额成交
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`      VARCHAR(32)   NOT NULL COMMENT '订单编号',
  `item_id`       BIGINT        NOT NULL COMMENT '物品ID',
  `buyer_id`      BIGINT        NOT NULL COMMENT '买家ID',
  `seller_id`     BIGINT        NOT NULL COMMENT '卖家ID',
  `price`         DECIMAL(10,2) NOT NULL COMMENT '成交价(元)',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0待付款 1待发货 2待收货 3已完成 4已取消',
  `contact_name`  VARCHAR(50)   DEFAULT NULL COMMENT '收货人',
  `contact_phone` VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
  `address`       VARCHAR(255)  DEFAULT NULL COMMENT '收货地址',
  `remark`        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `pay_time`      DATETIME      DEFAULT NULL COMMENT '支付时间',
  `ship_time`     DATETIME      DEFAULT NULL COMMENT '发货时间',
  `finish_time`   DATETIME      DEFAULT NULL COMMENT '完成时间',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------------------------------------------------------
-- 6. 敏感词表 sensitive_word (违规内容审核: DFA算法词典)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '敏感词ID',
  `word`        VARCHAR(50) NOT NULL COMMENT '敏感词',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- ============================================================================
-- 种子数据(仅演示分类与敏感词; 账号由后端 DataInitializer 启动时自动创建,
-- 避免在 SQL 中硬编码 BCrypt 哈希)
-- ============================================================================
INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES
('教材书籍', 0, 1),
('数码产品', 0, 2),
('生活用品', 0, 3),
('服饰鞋包', 0, 4),
('运动户外', 0, 5),
('其他', 0, 99),
('大学教材', 1, 1),
('考研资料', 1, 2),
('小说杂志', 1, 3),
('手机平板', 2, 1),
('笔记本电脑', 2, 2),
('耳机音箱', 2, 3),
('宿舍神器', 3, 1),
('文具', 3, 2);

INSERT INTO `sensitive_word` (`word`) VALUES
('代考'), ('枪支'), ('毒品'), ('发票'), ('办证'), ('刷单'), ('赌博'), ('色情'), ('违禁品'), ('假币');
