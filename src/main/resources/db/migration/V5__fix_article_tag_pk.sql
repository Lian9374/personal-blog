-- =====================================================
-- V5: article_tag 增加代理主键
-- MyBatis Plus 不支持复合主键(@TableId 只能有一个), 补一个自增 id 并把
-- (article_id, tag_id) 收为唯一约束。
-- =====================================================

ALTER TABLE article_tag DROP CONSTRAINT article_tag_pkey;
ALTER TABLE article_tag ADD COLUMN id BIGSERIAL;
ALTER TABLE article_tag ADD PRIMARY KEY (id);
ALTER TABLE article_tag ADD CONSTRAINT uk_article_tag UNIQUE (article_id, tag_id);
