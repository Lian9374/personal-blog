-- =====================================================
-- V6: 版块图标(纯展示, 用 emoji 让版块卡片更有辨识度)
-- =====================================================

ALTER TABLE board ADD COLUMN icon VARCHAR(10) NOT NULL DEFAULT '📁';
UPDATE board SET icon = '📢' WHERE name = '公告';
UPDATE board SET icon = '💻' WHERE name = '技术交流';
UPDATE board SET icon = '💬' WHERE name = '生活闲聊';
UPDATE board SET icon = '☕' WHERE name = '灌水区';
UPDATE board SET icon = '🗂️' WHERE name = '综合讨论';
