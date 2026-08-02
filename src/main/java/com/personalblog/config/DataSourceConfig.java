package com.personalblog.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * 数据源配置:
 * 兼容 Render 注入的 DATABASE_URL (形如 postgres://user:pass@host:port/db[?sslmode=require])。
 * 拆出 主机/端口/库名/用户名/密码 后, 用 Hikari 分别设置。
 * 注意: PostgreSQL JDBC 驱动不支持在 jdbc:postgresql:// 的 // 形式里内嵌 user:password,
 *       会把 "user:password@host" 整体当主机名, 所以必须拆开设置。
 * 未设置 DATABASE_URL 时(本地开发), 回退到 application.yml 的 spring.datasource.*。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        // 去掉 postgres:// 前缀, 再补 "//" 让 URI 能解析出 authority
        String rest = databaseUrl.trim().replaceFirst("^postgres(ql)?://", "");
        URI uri = URI.create("//" + rest);

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl("jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + (uri.getPath() == null ? "" : uri.getPath())
                + (uri.getQuery() != null ? "?" + uri.getQuery() : ""));
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            ds.setUsername(parts[0]);
            if (parts.length > 1) {
                ds.setPassword(parts[1]);
            }
        }
        ds.setMaximumPoolSize(10);
        return ds;
    }
}
