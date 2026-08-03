# 云麓论坛（个人博客升级版）

课程设计项目：由个人博客系统升级而来的**论坛级网站**，基于 Spring Boot + MyBatis Plus + Thymeleaf。

## 技术栈

- **Spring Boot 3.3.5** + Spring MVC
- **MyBatis Plus 3.5.9**（含分页，需 `mybatis-plus-jsqlparser` 模块）
- **Flyway** 数据库迁移
- **Thymeleaf** 服务端模板渲染（中文界面）
- **PostgreSQL 16** + **BCrypt** 密码加密 + 会话登录
- **commonmark-java + jsoup**：Markdown 渲染 + XSS 白名单净化
- 前端自包含（无 CDN）：令牌化 CSS 设计系统（亮/暗双主题、响应式）+ 原生 JS 增强

## 功能

- **版块**：版块导航首页、版块帖子流（置顶优先、最新/精华筛选、浏览量）
- **帖子**：Markdown 编辑/发布（实时预览）、标签、关键词搜索（帖子+用户）
- **评论**：楼层号、回复嵌套（二级）、作者/楼主/管理员可删
- **用户**：注册/登录、头像（base64 存库，Render 无持久磁盘也不丢）、简介、公开个人主页、关注
- **互动**：点赞、收藏（均可再点取消）
- **通知**：被回复/被赞/被收藏/被关注站内提醒，导航栏未读红点 + 全部已读
- **管理后台** `/admin`：仪表盘、用户管理（改角色/封禁）、版块管理、帖子管理（置顶/精华/删除）、评论管理
- **UI**：亮/暗主题切换（localStorage 记忆）、移动端汉堡菜单、三档响应式断点

## 运行方式

前置条件：JDK 17+、Maven、本机 PostgreSQL（端口 5432）。

```bash
# 1. 创建数据库（只需一次）
# PowerShell:
#   & "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE personal_blog;"

# 2. 设置数据库密码环境变量（应用通过 PGPASSWORD 读取，仓库不含明文密码）
# PowerShell:   $env:PGPASSWORD="你的数据库密码"
# Git Bash:     export PGPASSWORD="你的数据库密码"

# 3. 启动
mvn spring-boot:run
```

浏览器访问 http://localhost:8080

- 种子账号：`admin` / `admin123`（管理员，登录后可见"后台"入口；可在个人中心改密码）
- 首次启动 Flyway 自动建表（V1–V5）+ 写入种子数据：3 个版块、标签、3 篇帖子、2 条评论
- 重置数据：`psql -U postgres -h localhost -c "DROP DATABASE IF EXISTS personal_blog; CREATE DATABASE personal_blog;"` 后重启应用

## 部署到 Render（云端，免费）

> 数据库直接使用 **Render 自带的托管 PostgreSQL（免费）**，蓝图 Apply 时自动创建并注入连接信息，**无需自备数据库、无需手动填环境变量**。

### 部署步骤

1. 删除 Render 上旧的误建服务（Static Site / Node.js Web Service）。
2. Render 控制台 → **New → Blueprint** → 选择本仓库 → **Apply**。
   蓝图（`render.yaml`）自动创建：Web Service（Docker 打包）+ 托管 PostgreSQL，并注入 `DATABASE_URL`。
3. 首次部署 Flyway 自动执行 V1–V5 建表 + 种子数据（`admin` / `admin123`）。
4. 访问 `https://personal-blog.onrender.com`。

### 部署适配

- `Dockerfile`：多阶段构建（Maven 打包 → JRE 17 运行）。
- `render.yaml`：Blueprint，Web Service + 托管 PostgreSQL + 健康检查 `/` + `DATABASE_URL` 注入。
- `DataSourceConfig`：读取 `DATABASE_URL`（`postgres://user:pass@host:port/db`）拆解建连；未设置时回退 `application.yml` 本地默认。
- `application.yml`：`server.port=${PORT:8080}`；连接支持 `PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD` 环境变量覆盖。

## 数据库设计

- `user`：+ role(USER/ADMIN)、avatar(TEXT base64)、bio、status(ACTIVE/BANNED)
- `article`：+ board_id(版块)、view_count/comment_count/like_count/favorite_count、is_pinned/is_essence
- `comment`：+ parent_id(回复)、floor(楼层)
- 新增：`board`、`tag`、`article_tag`、`article_like`、`favorite`、`follow`、`notification`

> 注：`user` 是 PostgreSQL 保留字，SQL 与实体均用双引号包裹（`"user"`）。

## 权限与安全

- 登录拦截器 **fail-closed**（公开路径白名单 + GET/POST 区分）；`/admin/**` 由管理员拦截器每次读库校验角色
- 帖子编辑/删除仅作者或管理员；评论删除限评论作者/楼主/管理员（Service 层校验）
- **XSS**：帖子正文走 Markdown → jsoup 白名单净化 → `th:utext` 输出；其余字段一律 `th:text` 自动转义
- 头像上传校验图片魔数 + ≤1MB；切换类操作 POST + PRG，不收 Referer 防 open-redirect

## 目录结构

```
src/main/java/com/personalblog
├── common/advice      GlobalModelAdvice(导航版块/未读红点全局模型)
├── common/exception   BusinessException + GlobalExceptionHandler
├── config/            MyBatis Plus / Web MVC / 数据源
├── controller/        User/Article/Comment/Board/Tag/Search/Notification/Admin*
├── entity/            User/Article/Comment + Board/Tag/Like/Favorite/Follow/Notification
├── interceptor/       LoginInterceptor(fail-closed) + AdminInterceptor
├── mapper/            BaseMapper + 注解 SQL(计数/置顶/楼层)
└── service/           Markdown/版块/标签/点赞/收藏/关注/通知/文章/评论/用户
src/main/resources
├── db/migration/      Flyway V1-V5
├── templates/         页面 + fragments(导航/侧栏/帖子行/评论/分页/后台侧栏)
├── static/css/style.css  设计系统(令牌/暗色/响应式)
└── static/js/app.js  暗色切换/移动端菜单/Markdown 预览/防连点
```
