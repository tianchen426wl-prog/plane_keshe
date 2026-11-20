package com.airticket.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** The same migration path is used by normal startup, tests and the setup CLI. */
public final class DatabaseMigrations {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseMigrations.class);
    private static final Set<String> BASE_TABLES = Set.of("admin", "menu", "user", "company", "hbxx", "jpdd", "hbxx_cabin", "seat_hold", "tdjp", "system", "order_log");
    private static final Set<String> MODERN_TABLES = Set.of("airport", "aircraft_layout", "layout_seat", "fare_policy", "flight_schedule", "flight_schedule_exception", "flight_generation_run", "flight_quote", "flight_seat", "order_passenger", "order_price_item", "seat_assignment_history", "ai_plan", "ai_call_audit");
    private DatabaseMigrations() { }

    public static void migrate(Flyway flyway, boolean requireEmpty) {
        // Named lock also covers legacy adoption before a Flyway history table exists.
        // It is held and released on this dedicated connection, independent of DDL commits.
        try (Connection lock = flyway.getConfiguration().getDataSource().getConnection()) {
            String schema;
            try (Statement s = lock.createStatement(); ResultSet r = s.executeQuery("SELECT DATABASE()")) {
                r.next(); schema = r.getString(1);
            }
            String lockName = "plane-schema:" + hash(schema);
            try (PreparedStatement s = lock.prepareStatement("SELECT GET_LOCK(?,60)")) {
                s.setString(1, lockName);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next() || r.getInt(1) != 1) throw new IllegalStateException("数据库初始化正在由另一个实例执行，请稍后重试");
                }
            }
            try {
                Set<String> tables = tables(lock);
                if (requireEmpty && !tables.isEmpty()) throw new IllegalStateException("安装命令要求空库；已有数据库请直接启动后端或使用升级命令");
                if (!tables.isEmpty()) {
                    if (!tables.containsAll(BASE_TABLES)) throw new IllegalStateException("目标库非空且不符合本项目完整基础结构，拒绝自动初始化。请核对DB_NAME或恢复完整备份");
                    if (!tables.contains("flyway_schema_history")) {
                        int version = legacyVersion(lock, tables);
                        if (version >= 5 && !tables.containsAll(MODERN_TABLES)) throw new IllegalStateException("旧库版本记录与实际表结构不符，拒绝自动接管，请检查未完成的迁移");
                        if (version >= 10 && !tables.contains("passenger_profile")) throw new IllegalStateException("V10旧库缺少常用乘机人表，拒绝接管未完成的迁移");
                        // Baseline is explicit and guarded; generic baseline-on-migrate stays off.
                        Flyway.configure().configuration(flyway.getConfiguration())
                                .baselineVersion(MigrationVersion.fromVersion(Integer.toString(version)))
                                .baselineDescription("Adopt existing plane_keshe V" + version)
                                .load().baseline();
                        LOG.info("Adopted existing project schema {} at V{}; existing business data is preserved", schema, version);
                    }
                }
                flyway.migrate();
                LOG.info("Project database {} is ready; Flyway migrations completed", schema);
            } finally {
                try (PreparedStatement s = lock.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    s.setString(1, lockName);s.execute();
                } catch (SQLException releaseFailure) {
                    LOG.warn("Database migration lock connection closed; MySQL releases session locks automatically");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("数据库初始化失败，请检查MySQL权限和连接；不会自动执行clean或删除业务数据", e);
        }
    }

    private static Set<String> tables(Connection c) throws SQLException {
        Set<String> names = new HashSet<>();
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'")) {
            while(r.next()) names.add(r.getString(1));
        }
        return names;
    }

    private static int legacyVersion(Connection c, Set<String> tables) throws SQLException {
        if (!tables.contains("schema_version")) {
            // A partial V2 must not be mistaken for an unmodified V1.
            try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user' AND column_name='auth_version'")) {
                r.next();if(r.getInt(1)>0)throw new IllegalStateException("旧库存在升级字段却没有schema_version，拒绝猜测版本");
            }
            return 1;
        }
        Set<Integer> versions = new HashSet<>();
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT version FROM schema_version")) {while(r.next()) versions.add(r.getInt(1));}
        int version=versions.stream().mapToInt(Integer::intValue).max().orElse(0);
        if(version<1||version>11)throw new IllegalStateException("无法识别旧库版本；当前可自动接管V1～V11，请核对schema_version");
        for(int i=2;i<=version;i++)if(!versions.contains(i))throw new IllegalStateException("旧库版本记录不连续，拒绝跳过缺失迁移V"+i);
        return version;
    }

    private static String hash(String schema) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(schema.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();for(int i=0;i<16;i++)result.append(String.format("%02x",digest[i]));return result.toString();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
