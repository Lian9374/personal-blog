-- =====================================================
-- V1: personal_blog 数据库结构
-- 说明: `user` 是 MySQL 保留字, 必须用反引号包裹
-- =====================================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '登录名(唯一)',
    `password`   VARCHAR(100) NOT NULL                COMMENT 'BCrypt加密后的密码',
    `nickname`   VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `email`      VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `article` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '作者用户ID',
    `title`       VARCHAR(200) NOT NULL                COMMENT '标题',
    `summary`     VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '摘要',
    `content`     TEXT         NOT NULL                COMMENT '正文',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

CREATE TABLE IF NOT EXISTS `comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id`  BIGINT       NOT NULL                COMMENT '所属文章ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '评论用户ID',
    `content`     VARCHAR(500) NOT NULL                COMMENT '评论内容',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
    CONSTRAINT `fk_comment_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
