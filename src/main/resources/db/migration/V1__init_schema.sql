-- =====================================================
-- V1: personal_blog 数据库结构 (PostgreSQL)
-- 说明: user 是 PostgreSQL 保留字, 必须用双引号包裹
-- 自增主键用 BIGSERIAL; 时间用 TIMESTAMP
-- =====================================================

CREATE TABLE "user" (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(50),
    email      VARCHAR(100),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE article (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES "user" (id),
    title       VARCHAR(200) NOT NULL,
    summary     VARCHAR(500) NOT NULL DEFAULT '',
    content     TEXT         NOT NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_article_user_id ON article (user_id);

CREATE TABLE comment (
    id          BIGSERIAL    PRIMARY KEY,
    article_id  BIGINT       NOT NULL REFERENCES article (id),
    user_id     BIGINT       NOT NULL REFERENCES "user" (id),
    content     VARCHAR(500) NOT NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_comment_article_id ON comment (article_id);
CREATE INDEX idx_comment_user_id   ON comment (user_id);
