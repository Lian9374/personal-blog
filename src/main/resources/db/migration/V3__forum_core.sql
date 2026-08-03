-- =====================================================
-- V3: 论坛核心扩展 (版块 / 文章计数与置顶精华 / 评论楼层与回复 / 用户角色头像)
-- 顺序敏感: 先建 board 并插入默认版块, 再给 article 加 board_id 回填后设 NOT NULL + FK
-- =====================================================

-- ---------- 版块 ----------
CREATE TABLE board (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200) NOT NULL DEFAULT '',
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_board_name UNIQUE (name)
);

-- 默认版块(显式 id=1 供历史文章回填) + 同步序列
INSERT INTO board (id, name, description, sort_order) VALUES (1, '综合讨论', '默认版块（历史文章归入此处）', 0);
SELECT setval(pg_get_serial_sequence('board', 'id'), (SELECT MAX(id) FROM board));

-- ---------- article 增强 ----------
-- 先加可空列 -> 回填 -> 设 NOT NULL -> 加 FK
ALTER TABLE article ADD COLUMN board_id BIGINT;
UPDATE article SET board_id = 1;
ALTER TABLE article ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE article ADD CONSTRAINT fk_article_board FOREIGN KEY (board_id) REFERENCES board (id);

ALTER TABLE article ADD COLUMN view_count     BIGINT  NOT NULL DEFAULT 0;
ALTER TABLE article ADD COLUMN comment_count  BIGINT  NOT NULL DEFAULT 0;
ALTER TABLE article ADD COLUMN like_count     BIGINT  NOT NULL DEFAULT 0;
ALTER TABLE article ADD COLUMN favorite_count BIGINT  NOT NULL DEFAULT 0;
ALTER TABLE article ADD COLUMN is_pinned      BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE article ADD COLUMN is_essence     BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_article_board_time ON article (board_id, is_pinned DESC, create_time DESC);

-- 评论数回填(去规范化)
UPDATE article a SET comment_count = t.cnt
FROM (SELECT article_id, COUNT(*) AS cnt FROM comment GROUP BY article_id) t
WHERE a.id = t.article_id;

-- ---------- comment 增强 ----------
ALTER TABLE comment ALTER COLUMN content TYPE VARCHAR(1000);
ALTER TABLE comment ADD COLUMN parent_id BIGINT;
ALTER TABLE comment ADD COLUMN floor INT NOT NULL DEFAULT 0;
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id);
CREATE INDEX idx_comment_parent ON comment (parent_id);
-- 楼层回填: 每篇文章按时间正序编号
UPDATE comment c SET floor = t.rn
FROM (SELECT id, row_number() OVER (PARTITION BY article_id ORDER BY create_time, id) AS rn FROM comment) t
WHERE c.id = t.id;
ALTER TABLE comment ADD CONSTRAINT uq_comment_article_floor UNIQUE (article_id, floor);

-- ---------- user 增强 ----------
ALTER TABLE "user" ADD COLUMN avatar TEXT;
ALTER TABLE "user" ADD COLUMN role   VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE "user" ADD COLUMN bio    VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE "user" ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE "user" ADD CONSTRAINT ck_user_role   CHECK (role   IN ('USER', 'ADMIN'));
ALTER TABLE "user" ADD CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'BANNED'));
-- 管理员回填
UPDATE "user" SET role = 'ADMIN' WHERE username = 'admin';
