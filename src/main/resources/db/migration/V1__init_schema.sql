-- Baseline schema,內容取自 Hibernate (ddl-auto=create) 對 MariaDB 11.4 產生的 DDL,
-- 僅調整建表順序 (滿足外鍵相依) 與約束命名 (改為可讀名稱) .
-- 欄位型別必須與 entity 對應,否則 ddl-auto=validate 會在啟動時失敗.
--
-- 注意:carts.product_id / order_items.product_id / inventory_logs.product_id 刻意沒有外鍵,
-- 這是 entity 上 @NotFound(action = IGNORE) 造成的 (商品軟刪除後關聯需可為 null) ,
-- 與 Hibernate 產生的結果一致,請勿補上外鍵.

CREATE TABLE `users` (
  `is_account_non_expired`     bit(1)       NOT NULL,
  `is_account_non_locked`      bit(1)       NOT NULL,
  `is_credentials_non_expired` bit(1)       NOT NULL,
  `is_enabled`                 bit(1)       NOT NULL,
  `created_at`                 datetime(6)  DEFAULT NULL,
  `deleted_at`                 datetime(6)  DEFAULT NULL,
  `id`                         bigint(20)   NOT NULL AUTO_INCREMENT,
  `last_login_at`              datetime(6)  DEFAULT NULL,
  `last_password_change_at`    datetime(6)  DEFAULT NULL,
  `updated_at`                 datetime(6)  DEFAULT NULL,
  `avatar_url`                 varchar(255) DEFAULT NULL,
  `email`                      varchar(255) NOT NULL,
  `name`                       varchar(255) DEFAULT NULL,
  `password_hash`              varchar(255) NOT NULL,
  `phone`                      varchar(255) DEFAULT NULL,
  `role`                       enum('CUSTOMER','CUSTOMER_SERVICE','FINANCE','MARKETING','ORDER_MANAGER','PRODUCT_MANAGER','SUPER_ADMIN') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `products` (
  `price`           decimal(10,2) NOT NULL,
  `stock_quantity`  int(11)       NOT NULL,
  `created_at`      datetime(6)   DEFAULT NULL,
  `deleted_at`      datetime(6)   DEFAULT NULL,
  `id`              bigint(20)    NOT NULL AUTO_INCREMENT,
  `updated_at`      datetime(6)   DEFAULT NULL,
  `version`         bigint(20)    DEFAULT NULL,
  `cover_image_url` varchar(255)  DEFAULT NULL,
  `name`            varchar(255)  NOT NULL,
  `description`     text          DEFAULT NULL,
  `status`          enum('OFF_SHELF','ON_SHELF','OUT_OF_STOCK') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coupons` (
  `discount_value` decimal(10,2) NOT NULL,
  `min_spend`      decimal(10,2) DEFAULT NULL,
  `usage_limit`    int(11)       DEFAULT NULL,
  `created_at`     datetime(6)   DEFAULT NULL,
  `id`             bigint(20)    NOT NULL AUTO_INCREMENT,
  `valid_from`     datetime(6)   DEFAULT NULL,
  `valid_to`       datetime(6)   DEFAULT NULL,
  `code`           varchar(255)  NOT NULL,
  `discount_type`  varchar(255)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupons_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `orders` (
  `total_amount`      decimal(10,2) NOT NULL,
  `coupon_id`         bigint(20)    DEFAULT NULL,
  `created_at`        datetime(6)   DEFAULT NULL,
  `id`                bigint(20)    NOT NULL AUTO_INCREMENT,
  `user_id`           bigint(20)    NOT NULL,
  `recipient_address` varchar(255)  NOT NULL,
  `recipient_name`    varchar(255)  NOT NULL,
  `recipient_phone`   varchar(255)  NOT NULL,
  `status`            enum('CANCELLED','COMPLETED','PAID','PENDING','SHIPPED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_orders_coupon_id` (`coupon_id`),
  KEY `idx_orders_user_id` (`user_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_items` (
  `price_at_purchase` decimal(10,2) NOT NULL,
  `quantity`          int(11)       NOT NULL,
  `id`                bigint(20)    NOT NULL AUTO_INCREMENT,
  `order_id`          bigint(20)    NOT NULL,
  `product_id`        bigint(20)    DEFAULT NULL,
  `cover_image_url`   varchar(255)  DEFAULT NULL,
  `product_name`      varchar(255)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order_id` (`order_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `carts` (
  `quantity`   int(11)     NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20)  NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id`    bigint(20)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_carts_user_id` (`user_id`),
  CONSTRAINT `fk_carts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payments` (
  `amount`         decimal(10,2) NOT NULL,
  `id`             bigint(20)    NOT NULL AUTO_INCREMENT,
  `order_id`       bigint(20)    NOT NULL,
  `paid_at`        datetime(6)   DEFAULT NULL,
  `payment_method` varchar(255)  NOT NULL,
  `status`         varchar(255)  DEFAULT NULL,
  `transaction_id` varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_order_id` (`order_id`),
  CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shipments` (
  `delivered_at`        datetime(6)  DEFAULT NULL,
  `id`                  bigint(20)   NOT NULL AUTO_INCREMENT,
  `order_id`            bigint(20)   NOT NULL,
  `shipped_at`          datetime(6)  DEFAULT NULL,
  `logistics_provider`  varchar(255) DEFAULT NULL,
  `status`              varchar(255) DEFAULT NULL,
  `tracking_number`     varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipments_order_id` (`order_id`),
  CONSTRAINT `fk_shipments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reviews` (
  `rating`     int(11)     NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20)  NOT NULL,
  `user_id`    bigint(20)  NOT NULL,
  `comment`    text        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reviews_product_id` (`product_id`),
  KEY `idx_reviews_user_id` (`user_id`),
  CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reviews_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_images` (
  `sort_order` int(11)      DEFAULT NULL,
  `id`         bigint(20)   NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20)   NOT NULL,
  `image_url`  varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_images_product_id` (`product_id`),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inventory_logs` (
  `change_amount` int(11)      NOT NULL,
  `created_at`    datetime(6)  DEFAULT NULL,
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
  `operator_id`   bigint(20)   DEFAULT NULL,
  `product_id`    bigint(20)   NOT NULL,
  `reason`        varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
