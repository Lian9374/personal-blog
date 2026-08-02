# 个人博客系统

课程设计项目：一个基于 Spring Boot + MyBatis Plus + Thymeleaf 的个人博客系统。

## 技术栈

- **Spring Boot 3.3.5** + Spring MVC
- **MyBatis Plus 3.5.9**（含分页插件，需要 `mybatis-plus-jsqlparser` 模块）
- **Flyway** 数据库迁移
- **Thymeleaf** 服务端模板渲染（中文界面）
- **PostgreSQL 16** + **BCrypt** 密码加密 + 会话登录

> 注：原课程规格指定 MySQL，最终改用 PostgreSQL——功能完全等价，且让 Render 云端部署无需自备数据库（Render 自带免费托管 PostgreSQL），本地开发同样适用。

## 功能

- 用户：注册、登录、登出、修改资料（昵称/邮箱/密码）
- 文章：发布、编辑、删除（仅作者）、列表分页、详情
- 评论：发表评论、文章详情页展示评论

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

- 种子账号：`admin` / `admin123`（登录后可在个人中心改密码）
- 首次启动会自动通过 Flyway 建表 + 写入种子数据
- 重置数据：`psql -U postgres -h localhost -c "DROP DATABASE IF EXISTS personal_blog; CREATE DATABASE personal_blog;"` 后重启应用

## 部署到 Render（云端，免费）

> 数据库直接使用 **Render 自带的托管 PostgreSQL（免费）**，蓝图 Apply 时自动创建并注入连接信息，**无需自备数据库、无需手动填环境变量**。

### 部署失败原因回顾

1. **Static Site（静态站点）误建** → 报错 `Publish directory dist does not exist!`。本项目是 Spring Boot 后端，不产出 `dist`，不能用静态站点方式部署。
2. **Node.js Web Service 误建** → 报错 `Couldn't find a package.json`。Web Service 的运行时在创建那一刻锁死；当时仓库还没有 Dockerfile，Render 默认成了 Node。运行时无法就地修改，必须删除重建。
3. **MySQL 连接不上** → 报错 `Communications link failure / Connection refused`。Render 只托管 PostgreSQL/Redis，没有 MySQL；而应用启动时 Flyway 立即连库，连不上就启动失败。最终方案：**改用 PostgreSQL + Render 托管数据库**。

### 正确部署步骤

1. **删除** Render 上之前误建的 Static Site / Node.js Web Service。
2. Render 控制台 → **New → Blueprint** → 选择本仓库 → **Apply**。
   蓝图（`render.yaml`）会自动创建：
   - Web Service（`runtime: docker`，走 Dockerfile 用 Maven 打包）
   - 托管 PostgreSQL 数据库，并把 `DATABASE_URL` 自动注入服务
3. 等待首次部署完成（首次启动 Flyway 自动建表 + 写入种子数据 `admin` / `admin123`）。
4. 访问 `https://personal-blog.onrender.com`。

### 本仓库为部署已做的适配

- `Dockerfile`：多阶段构建（Maven 打包 → JRE 17 运行），Render 无 Java 原生运行时，必须走 Docker。
- `render.yaml`：Blueprint 蓝图，Web Service + 托管 PostgreSQL + 健康检查 `/` + `DATABASE_URL` 自动注入。
- `DataSourceConfig`：应用直接读取 `DATABASE_URL`（`postgres://user:pass@host:port/db`），拆出主机/库名/账号/密码建连；未设置时回退 `application.yml`（本地开发）。
- `application.yml`：`server.port` 支持 `${PORT:8080}`（Render 会注入 PORT）；数据库连接支持 `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` 环境变量覆盖，本地不设时回退本机默认。
- `mvnw`：Maven wrapper，便于在无 Maven 环境构建。

> 本地开发不受影响：不设环境变量时连接本机 `localhost:5432/personal_blog`（postgres 用户，密码通过 `PGPASSWORD` 提供）。

## 数据库设计

- `user`：id, username(唯一), password(BCrypt), nickname, email, created_at
- `article`：id, user_id(FK→user), title, summary, content, create_time, update_time
- `comment`：id, article_id(FK→article), user_id(FK→user), content, create_time

> 注：`user` 是 PostgreSQL 保留字，SQL 与 MyBatis Plus 实体中均用双引号包裹（`"user"`）。

## 目录结构

```
src/main/java/com/personalblog
├── config/          MyBatis Plus / Web MVC 配置
├── controller/      User / Article / Comment 控制器
├── entity/          实体（Lombok + MyBatis Plus 注解）
├── mapper/          BaseMapper 接口
├── service/         业务层
├── interceptor/     登录拦截器
└── common/exception 业务异常 + 全局异常处理
src/main/resources
├── db/migration/    Flyway SQL（V1 建表, V2 种子数据）
├── templates/       Thymeleaf 页面
└── static/css/      样式（自包含，无 CDN）
```

## 权限说明

- 登录拦截器保护：`/profile`、`/article/create`、编辑/删除、`/comment/add`
- 文章编辑/删除仅限作者本人（Service 层校验，非作者返回 403）
- 评论/文章正文使用 `th:text` 渲染，自动转义防 XSS
