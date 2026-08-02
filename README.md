# 个人博客系统

课程设计项目：一个基于 Spring Boot + MyBatis Plus + Thymeleaf 的个人博客系统。

## 技术栈

- **Spring Boot 3.3.5** + Spring MVC
- **MyBatis Plus 3.5.9**（含分页插件，需要 `mybatis-plus-jsqlparser` 模块）
- **Flyway** 数据库迁移
- **Thymeleaf** 服务端模板渲染（中文界面）
- **MySQL 8** + **BCrypt** 密码加密 + 会话登录

## 功能

- 用户：注册、登录、登出、修改资料（昵称/邮箱/密码）
- 文章：发布、编辑、删除（仅作者）、列表分页、详情
- 评论：发表评论、文章详情页展示评论

## 运行方式

前置条件：JDK 17+、Maven、本机 MySQL 8（端口 3306）。

```bash
# 1. 设置数据库密码环境变量（应用通过 DB_PASSWORD 读取，仓库不含明文密码）
# PowerShell:   $env:DB_PASSWORD="你的数据库密码"
# Git Bash:     export DB_PASSWORD="你的数据库密码"

# 2. 启动
mvn spring-boot:run
```

浏览器访问 http://localhost:8080

- 种子账号：`admin` / `admin123`（登录后可在个人中心改密码）
- 首次启动会自动创建数据库 `personal_blog` 并通过 Flyway 建表 + 写入种子数据
- 重置数据：`mysql -uroot -p -e "DROP DATABASE IF EXISTS personal_blog;"` 后重启应用

## 部署到 Render（云端）

> ⚠️ **重要**：Render 只有 PostgreSQL/Redis 托管数据库，**没有 MySQL**。本应用依赖 MySQL，需要你自备一个**外部可访问的 MySQL**，通过环境变量告诉应用连接。

### 部署失败原因回顾

之前部署报错 `Publish directory dist does not exist!`，是因为在 Render 上建成了 **Static Site（静态站点）**，而本项目是 Spring Boot 后端服务，不会产出 `dist` 目录，也不该用静态站点方式部署。

### 正确部署步骤

1. **删除** Render 上之前误建的 Static Site。
2. **准备外部 MySQL**（任选其一）：
   - 免费：Aiven for MySQL（Free 档）、db4free 等；
   - 已有的云 MySQL（阿里云/腾讯云/自建）。
   创建好数据库后，把连接串整理成 `jdbc:mysql://主机:端口/库名?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`。
3. **使用仓库里的 `render.yaml` 蓝图部署**：Render 控制台 → **New → Blueprint** → 选择本仓库 → Apply。
   蓝图会按 `runtime: docker` 创建 Web Service（多阶段 Dockerfile 自动用 Maven 打包）。
4. Apply 后在 **Environment** 里填入三个环境变量：
   | 变量 | 值 |
   |---|---|
   | `DB_URL` | 上面第 2 步的 JDBC 连接串 |
   | `DB_USERNAME` | MySQL 用户名 |
   | `DB_PASSWORD` | MySQL 密码 |
5. 手动触发一次 **Deploy**。首次启动 Flyway 会自动建表并写入种子数据（admin/admin123）。

### 本仓库为部署已做的适配

- `Dockerfile`：多阶段构建（Maven 打包 → JRE 17 运行），Render 无 Java 原生运行时，必须走 Docker。
- `render.yaml`：蓝图配置，Web Service + 健康检查 `/` + 三个环境变量占位。
- `application.yml`：`server.port` 支持 `${PORT:8080}`（Render 会注入 PORT）；数据库连接支持 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 环境变量覆盖，本地不设时回退本机默认。
- `mvnw`：Maven wrapper，便于在无 Maven 环境构建。

> 本地开发不受影响：不设环境变量时仍连接本机 `localhost:3306/personal_blog`（需另设 `DB_PASSWORD`）。

## 数据库设计

- `user`：id, username(唯一), password(BCrypt), nickname, email, created_at
- `article`：id, user_id(FK→user), title, summary, content, create_time, update_time
- `comment`：id, article_id(FK→article), user_id(FK→user), content, create_time

> 注：`user` 是 MySQL 保留字，SQL 与 MyBatis Plus 实体中均用反引号包裹。

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
