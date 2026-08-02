-- =====================================================
-- V2: 种子数据 (1 个管理员 + 3 篇文章 + 2 条评论)
-- 密码: admin123  (BCrypt, 已用 spring-security-crypto 生成并验证)
-- =====================================================

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`) VALUES
(1, 'admin', '$2a$10$NFHA.2jpaJ5bRDdmi2qjB.sUtnOkCDt6OovYpCb/ZHO2RUkXyITL6', '管理员', 'admin@example.com');

INSERT INTO `article` (`id`, `user_id`, `title`, `summary`, `content`) VALUES
(1, 1, '我的第一篇博客', '介绍个人博客系统的功能和设计。', '这是一篇示例文章，用于演示个人博客系统的文章详情、评论等功能。'),
(2, 1, 'Spring Boot 3 快速入门', '简述 Spring Boot 3 + MyBatis Plus 的开发流程。', 'Spring Boot 3 要求 Java 17+，配合 MyBatis Plus 可以快速实现 CRUD。'),
(3, 1, '课程设计心得体会', '记录完成本课程设计的收获。', '通过这个项目，我掌握了分层架构、Flyway 迁移、Thymeleaf 渲染和会话登录等技能。');

INSERT INTO `comment` (`id`, `article_id`, `user_id`, `content`) VALUES
(1, 1, 1, '欢迎来到个人博客！'),
(2, 2, 1, '这篇入门文章写得很清楚。');
