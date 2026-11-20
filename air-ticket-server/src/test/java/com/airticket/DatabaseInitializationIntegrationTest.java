package com.airticket;

import com.airticket.database.DatabaseCreator;
import com.airticket.database.DatabaseMigrations;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Every database name is freshly allocated and verified absent before this test owns it. */
class DatabaseInitializationIntegrationTest {
    private static final String SERVER="jdbc:mysql://127.0.0.1:3306/";
    private static final String OPTIONS="?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8";
    private final Set<String> owned = new HashSet<>();
    private final String user=Optional.ofNullable(System.getenv("DB_USERNAME")).orElse("root");
    private String password;

    @BeforeEach void credentials() throws Exception {
        password=System.getenv("DB_PASSWORD");
        if(password==null){
            for(Path p:List.of(Paths.get(".local/application-local.yml"),Paths.get("../.local/application-local.yml"))){
                if(Files.exists(p)){
                    // Do not use Spring's debug-logging YAML loader for files containing secrets.
                    try(java.io.Reader reader=Files.newBufferedReader(p)){
                        Map<?,?> document=new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor()).load(reader);
                        Object local=document.get("local");
                        Object value=local instanceof Map?((Map<?,?>)local).get("db-password"):null;
                        if(value!=null){password=value.toString();break;}
                    }
                }
            }
        }
        assertNotNull(password,"Provide DB_PASSWORD or local.db-password for local MySQL tests");
    }
    private String allocate() throws Exception {
        String name="air_ticket_boot_it_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        try(Connection c=server();PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?")){
            q.setString(1,name);try(ResultSet r=q.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
        }
        owned.add(name);return name;
    }
    private Connection server()throws SQLException{return DriverManager.getConnection(SERVER+OPTIONS,user,password);}
    private String url(String name){return SERVER+name+OPTIONS;}
    private Flyway flyway(String name){return Flyway.configure().dataSource(url(name),user,password).locations("classpath:db/migration").encoding("UTF-8").cleanDisabled(true).baselineOnMigrate(false).placeholderReplacement(false).load();}
    private JdbcTemplate db(String name){return new JdbcTemplate(new DriverManagerDataSource(url(name),user,password));}
    private void create(String name)throws Exception{try(Connection c=server();Statement s=c.createStatement()){s.execute("CREATE DATABASE `"+name+"` CHARACTER SET utf8mb4");}}
    private int n(JdbcTemplate db,String sql,Object...args){return db.queryForObject(sql,Integer.class,args);}
    private int latestVersion(String name){return Arrays.stream(flyway(name).info().all()).filter(m->m.getVersion()!=null).mapToInt(m->Integer.parseInt(m.getVersion().toString())).max().orElseThrow();}
    @AfterEach void cleanup()throws Exception{
        for(String name:owned){assertTrue(name.matches("air_ticket_boot_it_[a-f0-9]{12}"));try(Connection c=server();Statement s=c.createStatement()){s.execute("DROP DATABASE IF EXISTS `"+name+"`");}}
    }
    private void businessFixture(JdbcTemplate db){
        db.update("INSERT INTO company(company_name,company_profile)VALUES('成员自行维护航司','保留此内容')");int company=n(db,"SELECT company_id FROM company WHERE company_name='成员自行维护航司'");
        db.update("INSERT INTO `user`(username,password,name)VALUES('member','unused-test-hash','成员测试')");int member=n(db,"SELECT id FROM `user` WHERE username='member'");
        db.update("INSERT INTO hbxx(company_id,start,`end`,starttime,number,price,flight_no)VALUES(?,'测试出发','测试到达',DATE_ADD(NOW(),INTERVAL 3 DAY),0,100,'PRESERVED-FLIGHT')",company);int flight=n(db,"SELECT hbid FROM hbxx WHERE flight_no='PRESERVED-FLIGHT'");
        db.update("INSERT INTO hbxx_cabin(hbid,cabin_class,cabin_name,price,total_number,available_number)VALUES(?,1,'经济舱',100,10,9)",flight);int cabin=n(db,"SELECT cabin_id FROM hbxx_cabin WHERE hbid=?",flight);
        db.update("INSERT INTO jpdd(hbid,userid,cabin_id,quantity,cost,unit_price,status,order_no)VALUES(?,?,?,1,100,100,1,'PRESERVED-ORDER')",flight,member,cabin);
    }
    private void preserved(JdbcTemplate db){
        assertEquals(1,n(db,"SELECT COUNT(*) FROM jpdd WHERE order_no='PRESERVED-ORDER' AND cost=100 AND status=1"));
        assertEquals(9,n(db,"SELECT available_number FROM hbxx_cabin WHERE hbid=(SELECT hbid FROM hbxx WHERE flight_no='PRESERVED-FLIGHT')"));
        assertEquals("保留此内容",db.queryForObject("SELECT company_profile FROM company WHERE company_name='成员自行维护航司'",String.class));
    }

    @Test void missingDatabaseIsCreatedAndRepeatStartupPreservesData()throws Exception{
        String name=allocate();DatabaseCreator.ensureDatabase(url(name),user,password,true);DatabaseMigrations.migrate(flyway(name),false);JdbcTemplate db=db(name);
        assertEquals(latestVersion(name),n(db,"SELECT MAX(version) FROM schema_version"));assertEquals(latestVersion(name),n(db,"SELECT COUNT(*) FROM flyway_schema_history WHERE success=1"));
        assertEquals(14,n(db,"SELECT COUNT(*) FROM company"));assertEquals(65,n(db,"SELECT COUNT(*) FROM airport"));assertEquals(364,n(db,"SELECT COUNT(*) FROM flight_schedule WHERE enabled=1"));
        assertEquals(0,n(db,"SELECT COUNT(*) FROM `user`"));assertEquals(0,n(db,"SELECT COUNT(*) FROM hbxx"));assertEquals(1,n(db,"SELECT COUNT(*) FROM admin"));
        String hash=db.queryForObject("SELECT password FROM admin WHERE username='admin'",String.class);
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("admin123",hash));
        businessFixture(db);db.update("UPDATE admin SET password='member-changed-password' WHERE username='admin'");
        DatabaseCreator.ensureDatabase(url(name),user,password,false);DatabaseMigrations.migrate(flyway(name),false);preserved(db);
        assertEquals("member-changed-password",db.queryForObject("SELECT password FROM admin WHERE username='admin'",String.class));assertEquals(latestVersion(name),n(db,"SELECT COUNT(*) FROM flyway_schema_history"));
    }
    @Test void precreatedEmptyDatabaseIsInitialized()throws Exception{
        String name=allocate();create(name);DatabaseCreator.ensureDatabase(url(name),user,password,false);DatabaseMigrations.migrate(flyway(name),true);assertEquals(14,n(db(name),"SELECT COUNT(*) FROM company"));
    }
    @ParameterizedTest @ValueSource(ints={4,8,9,10})
    void existingLegacyVersionsAreAdoptedWithoutReset(int version)throws Exception{
        String name=allocate();create(name);Flyway.configure().configuration(flyway(name).getConfiguration()).target(Integer.toString(version)).load().migrate();JdbcTemplate db=db(name);businessFixture(db);
        // Simulate a database created by the previous SQL scripts, before Flyway adoption.
        db.execute("DROP TABLE flyway_schema_history");DatabaseMigrations.migrate(flyway(name),false);preserved(db);
        assertEquals(latestVersion(name),n(db,"SELECT MAX(version) FROM schema_version"));assertEquals(version,n(db,"SELECT version FROM flyway_schema_history WHERE type='BASELINE'"));assertEquals(14,n(db,"SELECT COUNT(*) FROM company WHERE airline_key IS NOT NULL"));
    }
    @Test void unrelatedDatabaseIsRefusedWithoutCreatingProjectTables()throws Exception{
        String name=allocate();create(name);JdbcTemplate db=db(name);db.execute("CREATE TABLE member_notes(id INT PRIMARY KEY)");db.update("INSERT INTO member_notes VALUES(7)");
        assertThrows(IllegalStateException.class,()->DatabaseMigrations.migrate(flyway(name),false));assertEquals(7,n(db,"SELECT id FROM member_notes"));assertEquals(1,n(db,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"));
    }
    @Test void disabledCreationDoesNotCreateMissingDatabase()throws Exception{
        String name=allocate();assertThrows(IllegalStateException.class,()->DatabaseCreator.ensureDatabase(url(name),user,password,false));
        try(Connection c=server();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?")){s.setString(1,name);try(ResultSet r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}}
    }
    @Test void installCommandRefusesNonemptyProjectDatabase()throws Exception{
        String name=allocate();create(name);DatabaseMigrations.migrate(flyway(name),false);assertThrows(IllegalStateException.class,()->DatabaseMigrations.migrate(flyway(name),true));assertEquals(1,n(db(name),"SELECT COUNT(*) FROM admin"));
    }
    @Test void twoInitializersDoNotDuplicateCatalog()throws Exception{
        String name=allocate();ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Callable<Void> task=()->{DatabaseCreator.ensureDatabase(url(name),user,password,true);DatabaseMigrations.migrate(flyway(name),false);return null;};
            List<Future<Void>> results=List.of(pool.submit(task),pool.submit(task));for(Future<Void> result:results)result.get(60,TimeUnit.SECONDS);
            assertEquals(14,n(db(name),"SELECT COUNT(*) FROM company"));assertEquals(364,n(db(name),"SELECT COUNT(*) FROM flight_schedule WHERE enabled=1"));assertEquals(latestVersion(name),n(db(name),"SELECT COUNT(*) FROM flyway_schema_history"));
        }finally{pool.shutdownNow();}
    }
    @Test void checksumMismatchStopsMigration()throws Exception{
        String name=allocate();create(name);DatabaseMigrations.migrate(flyway(name),false);JdbcTemplate db=db(name);db.update("UPDATE flyway_schema_history SET checksum=checksum+1 WHERE version='8'");
        assertThrows(FlywayException.class,()->DatabaseMigrations.migrate(flyway(name),false));assertEquals(14,n(db,"SELECT COUNT(*) FROM company"));
    }
    @Test void unsafeDatabaseNamesAreRejectedBeforeConnection(){
        assertThrows(IllegalArgumentException.class,()->DatabaseCreator.ensureDatabase("jdbc:mysql://127.0.0.1:3306/air_ticket;DROP_DATABASE",user,password,true));
    }
}
