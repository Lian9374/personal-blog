-- =====================================================
-- V4: 论坛互动扩展 (标签 / 点赞 / 收藏 / 关注 / 通知)
-- =====================================================

-- ---------- 标签 ----------
CREATE TABLE tag (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tag_name UNIQUE (name)
);

CREATE TABLE article_tag (
    article_id BIGINT NOT NULL REFERENCES article (id),
    tag_id     BIGINT NOT NULL REFERENCES tag (id),
    PRIMARY KEY (article_id, tag_id)
);
CREATE INDEX idx_article_tag_tag ON article_tag (tag_id);

-- ---------- 点赞 / 收藏 / 关注 ----------
CREATE TABLE article_like (
    id          BIGSERIAL   PRIMARY KEY,
    article_id  BIGINT      NOT NULL REFERENCES article (id),
    user_id     BIGINT      NOT NULL REFERENCES "user" (id),
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_article_like UNIQUE (article_id, user_id)
);
CREATE INDEX idx_article_like_user ON article_like (user_id);

CREATE TABLE favorite (
    id          BIGSERIAL   PRIMARY KEY,
    article_id  BIGINT      NOT NULL REFERENCES article (id),
    user_id     BIGINT      NOT NULL REFERENCES "user" (id),
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_favorite UNIQUE (article_id, user_id)
);
CREATE INDEX idx_favorite_user ON favorite (user_id);

CREATE TABLE follow (
    id           BIGSERIAL   PRIMARY KEY,
    follower_id  BIGINT      NOT NULL REFERENCES "user" (id),
    following_id BIGINT      NOT NULL REFERENCES "user" (id),
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_follow UNIQUE (follower_id, following_id),
    CONSTRAINT ck_follow_no_self CHECK (follower_id <> following_id)
);
CREATE INDEX idx_follow_following ON follow (following_id);
CREATE INDEX idx_follow_follower  ON follow (follower_id);

-- ---------- 通知 ----------
CREATE TABLE notification (
    id           BIGSERIAL   PRIMARY KEY,
    recipient_id BIGINT      NOT NULL REFERENCES "user" (id),
    actor_id     BIGINT      NOT NULL REFERENCES "user" (id),
    type         VARCHAR(20) NOT NULL,
    article_id   BIGINT,
    comment_id   BIGINT,
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notification_type CHECK (type IN ('REPLY', 'LIKE', 'FAVORITE', 'FOLLOW'))
);
CREATE INDEX idx_notification_recipient ON notification (recipient_id, is_read, create_time DESC);

-- ---------- 种子: 常用标签 + 更多版块 ----------
INSERT INTO tag (name) VALUES ('学习笔记'), ('编程'), ('生活');
INSERT INTO board (name, description, sort_order) VALUES
('技术交流', '技术问题讨论与分享', 1),
('灌水区', '轻松闲聊', 2);
