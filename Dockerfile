# ============================================================
# 个人博客 - 多阶段构建
# 构建阶段: Maven 3.9 + JDK 17 编译打包
# 运行阶段: 精简 JRE 17 运行时
# ============================================================

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝 pom.xml 并预下载依赖(利用 Docker 层缓存, 加速后续构建)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 再拷贝源码并打包
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/personal-blog-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
