-- 西湖论剑平台：AI 助手与在线支付模块（MySQL 8.0）
-- 说明：服务启动时 JPA ddl-auto=update 也会自动建表；本脚本用于手工初始化/交付审查。

CREATE TABLE IF NOT EXISTS `payment_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) DEFAULT NULL COMMENT 'TICKET/MATERIAL/MERCH',
  `price` decimal(10,2) NOT NULL,
  `stock` int DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `status` int DEFAULT 1 COMMENT '1 在售',
  `sort_order` int DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) NOT NULL,
  `user_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(20) NOT NULL COMMENT 'CREATED/PAID/CLOSED',
  `channel` varchar(255) DEFAULT NULL COMMENT 'MOCK/ALIPAY/WECHAT',
  `pay_url` varchar(255) DEFAULT NULL,
  `trade_no` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment_callback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(255) NOT NULL,
  `channel` varchar(255) DEFAULT NULL,
  `trade_no` varchar(255) DEFAULT NULL,
  `payload` varchar(2000) DEFAULT NULL,
  `success` tinyint(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_callback_order` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_chat_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `question` varchar(2000) DEFAULT NULL,
  `answer` varchar(4000) DEFAULT NULL,
  `model` varchar(255) DEFAULT NULL,
  `latency_ms` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_log_user` (`user_id`),
  KEY `idx_ai_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 演示商品（服务端 PaymentDataSeeder 也会幂等写入）
INSERT INTO `payment_product` (`name`, `type`, `price`, `stock`, `description`, `status`, `sort_order`)
SELECT * FROM (
  SELECT '西湖论剑大会 · VIP 通票' AS name, 'TICKET' AS type, 1280.00 AS price, 500 AS stock,
         '含主论坛 + 全部分论坛、午餐与大会资料包，可开具电子发票。' AS description, 1 AS status, 1 AS sort_order
  UNION ALL
  SELECT 'AI 安全分论坛通行证', 'TICKET', 399.00, 800, '单场分论坛入场资格，含同声传译与茶歇。', 1, 2
  UNION ALL
  SELECT '大会嘉宾 PPT 资料包', 'MATERIAL', 99.00, 2000, '主论坛与分论坛嘉宾授权 PPT 合集（PDF）。', 1, 3
) t
WHERE NOT EXISTS (SELECT 1 FROM `payment_product`);
