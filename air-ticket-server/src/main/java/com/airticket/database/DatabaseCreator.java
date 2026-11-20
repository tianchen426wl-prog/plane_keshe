package com.airticket.database;

import java.sql.*;
import java.util.Properties;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates only the explicitly configured MySQL schema, and only on error 1049. */
public final class DatabaseCreator {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseCreator.class);
    private static final Pattern URL = Pattern.compile("^(jdbc:mysql://[^/?#]+)/([A-Za-z0-9_]{1,64})(\\?[^#]*)?$");
    private DatabaseCreator() { }

    public static void ensureDatabase(String url, String username, String password, boolean createIfMissing) {
        Matcher target = URL.matcher(url);
        if (!target.matches()) {
            throw new IllegalArgumentException("自动初始化要求 jdbc:mysql://主机:端口/数据库名 格式，库名只能包含字母、数字或下划线");
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", username);
        credentials.setProperty("password", password == null ? "" : password);
        credentials.setProperty("connectTimeout", "10000");
        credentials.setProperty("socketTimeout", "30000");
        try (Connection ignored = DriverManager.getConnection(url, credentials)) {
            return; // Existing databases do not require CREATE DATABASE privileges.
        } catch (SQLException e) {
            if (e.getErrorCode() != 1049) {
                throw new IllegalStateException("MySQL连接失败，请检查服务、地址、账号与密码；不会尝试清空或重建数据库（错误码 " + e.getErrorCode() + "）", e);
            }
            if (!createIfMissing) {
                throw new IllegalStateException("目标数据库不存在，自动建库已关闭；请创建数据库或启用 DB_CREATE_IF_MISSING", e);
            }
        }
        String database = target.group(2);
        String serverUrl = target.group(1) + "/" + (target.group(3) == null ? "" : target.group(3));
        try (Connection connection = DriverManager.getConnection(serverUrl, credentials);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            LOG.info("Created missing project database {}; schema migrations will run next", database);
        } catch (SQLException e) {
            throw new IllegalStateException("自动创建数据库失败：请确认当前MySQL账号具有CREATE DATABASE权限；未修改其他数据库", e);
        }
    }
}
