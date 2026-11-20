package com.airticket;

import com.airticket.config.ApiException;
import com.airticket.service.BookingService;
import com.airticket.util.PasswordUtil;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/air_ticket_test?serverTimezone=Asia/Shanghai&useSSL=false", "jwt.secret=Y291cnNlLXRlc3Qtc2VjcmV0LWZvci1pc29sYXRlZC10ZXN0cy1vbmx5", "booking.hold-seconds=900","flights.generation.enabled=false","ai.enabled=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingIntegrationTest {
 @Autowired JdbcTemplate db; @Autowired BookingService booking; @Autowired MockMvc mvc;
 @Autowired ObjectMapper json; @Autowired PasswordUtil passwords;
 @Autowired com.airticket.service.HbxxService flights;
 @Autowired com.airticket.service.HbxxCabinService cabins;
 @Autowired com.airticket.mapper.AdminMapper admins;
 int user,other,flight,cabin,admin;String token,adminToken,tag;
 int number(String sql,Object...args){return db.queryForObject(sql,Integer.class,args);}
 int insert(String sql,Object...args){db.update(sql,args);return number("SELECT LAST_INSERT_ID()");}
 @BeforeAll void guard(){assertEquals("air_ticket_test",db.queryForObject("SELECT DATABASE()",String.class),"Tests must never run in the business database");}
 @BeforeEach void fixtures() throws Exception {
  tag="it"+UUID.randomUUID().toString().replace("-","").substring(0,14);
  String hash=passwords.encode("test123456");
  db.update("INSERT INTO `user`(username,password,name)VALUES(?,?,?)",tag,hash,"集成测试");user=number("SELECT id FROM `user` WHERE username=?",tag);
  db.update("INSERT INTO `user`(username,password,name)VALUES(?,?,?)",tag+"b",hash,"用户B");other=number("SELECT id FROM `user` WHERE username=?",tag+"b");
  db.update("INSERT INTO admin(username,password,role,menu_id)VALUES(?,?,1,'4')",tag,hash);admin=number("SELECT id FROM admin WHERE username=?",tag);
  db.update("INSERT INTO admin_menu(admin_id,menu_id)SELECT ?,id FROM menu WHERE path='/admin/flight'",admin);
  db.update("INSERT INTO hbxx(company_id,xclb,start,`end`,starttime,number,price,duration,place,flight_no)VALUES(NULL,'国内','测试出发','测试到达',DATE_ADD(NOW(),INTERVAL 2 DAY),10,100,'2h','测试机场',?)",tag);
  flight=number("SELECT hbid FROM hbxx WHERE flight_no=?",tag);
  db.update("INSERT INTO hbxx_cabin(hbid,cabin_class,cabin_name,price,total_number,available_number)VALUES(?,1,'经济舱',100,10,10)",flight);
  cabin=number("SELECT cabin_id FROM hbxx_cabin WHERE hbid=?",flight);
  token=login("user",tag);adminToken=login("admin",tag);
 }
 String login(String kind,String username)throws Exception{
  String content=mvc.perform(post("/api/"+kind+"/login").contentType("application/json").content(json.writeValueAsString(Map.of("username",username,"password","test123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  return "Bearer "+json.readTree(content).path("data").path("token").asText();
 }
 Long hold(int quantity){return booking.hold(flight,cabin,user,quantity,UUID.randomUUID().toString());}
 void invariant(){assertEquals(number("SELECT total_number FROM hbxx_cabin WHERE cabin_id=?",cabin),number("SELECT available_number+(SELECT COALESCE(SUM(quantity),0)FROM seat_hold WHERE cabin_id=? AND status=0)+(SELECT COALESCE(SUM(quantity),0)FROM jpdd WHERE cabin_id=? AND status IN(1,2))FROM hbxx_cabin WHERE cabin_id=?",cabin,cabin,cabin));}
 @AfterEach void cleanup(){
  db.update("DELETE FROM tdjp WHERE ddid IN(SELECT ddid FROM jpdd WHERE hbid=?)",flight);
  db.update("DELETE FROM jpdd WHERE hbid=?",flight);
  db.update("DELETE FROM order_log WHERE operator IN(?,?) OR ddid NOT IN(SELECT ddid FROM jpdd)",Integer.toString(user),Integer.toString(admin));
  db.update("DELETE FROM seat_hold WHERE hbid=?",flight);db.update("DELETE FROM hbxx_cabin WHERE hbid=?",flight);db.update("DELETE FROM hbxx WHERE hbid=?",flight);
  db.update("DELETE FROM admin_menu WHERE admin_id=?",admin);db.update("DELETE FROM admin WHERE id=?",admin);db.update("DELETE FROM `user` WHERE id IN(?,?)",user,other);
 }
 @Test void anonymousAndCrossTypeAreRejected()throws Exception{
  mvc.perform(get("/api/jpdd/myOrders")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/hbxx/add").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/role/list").header("Authorization",token)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/jpdd/myOrders").header("Authorization",adminToken)).andExpect(status().isUnauthorized());
 }
 @Test void menuAuthorizationAndCredentialVersion()throws Exception{
  mvc.perform(get("/api/admin/role/list").header("Authorization",adminToken)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/hbxx/search").header("Authorization",adminToken)).andExpect(status().isOk());
  db.update("UPDATE admin SET auth_version=auth_version+1 WHERE id=?",admin);
  mvc.perform(get("/api/admin/hbxx/search").header("Authorization",adminToken)).andExpect(status().isUnauthorized());
 }
 @Test void negativeZeroAndPastFlightAreRejected(){
  assertThrows(IllegalArgumentException.class,()->hold(-1));assertThrows(IllegalArgumentException.class,()->hold(0));assertThrows(IllegalArgumentException.class,()->hold(6));
  db.update("UPDATE hbxx SET starttime=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE hbid=?",flight);
  assertThrows(ApiException.class,()->hold(1));assertEquals(10,number("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));
 }
 @Test void duplicateHoldAndPaymentLockPrice(){
  String key=UUID.randomUUID().toString();Long id=booking.hold(flight,cabin,user,2,key);
  assertEquals(id,booking.hold(flight,cabin,user,2,key));
  db.update("UPDATE hbxx_cabin SET price=900 WHERE cabin_id=?",cabin);
  assertTrue(booking.confirm(id,user));assertTrue(booking.confirm(id,user));
  assertEquals(1,number("SELECT COUNT(*)FROM jpdd WHERE hold_id=?",id));assertEquals(200,number("SELECT cost FROM jpdd WHERE hold_id=?",id));invariant();
 }
 @Test void ownerCannotBeSpoofed()throws Exception{
  Long id=hold(1);assertThrows(ApiException.class,()->booking.cancel(id,other));
  mvc.perform(delete("/api/jpdd/hold/"+id).param("userid",Integer.toString(other)).header("Authorization",token)).andExpect(status().isOk());
  assertEquals(3,number("SELECT status FROM seat_hold WHERE hold_id=?",id));invariant();
 }
 @Test void expirationCommitsBeforeErrorResponse()throws Exception{
  Long id=hold(1);db.update("UPDATE seat_hold SET expire_time=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE hold_id=?",id);
  mvc.perform(post("/api/jpdd/create").header("Authorization",token).contentType("application/json").content("{\"holdId\":"+id+"}")).andExpect(status().isConflict());
  assertEquals(2,number("SELECT status FROM seat_hold WHERE hold_id=?",id));invariant();
 }
 @Test void lastSeatFiftyConcurrentRequests()throws Exception{
  db.update("UPDATE hbxx_cabin SET total_number=1,available_number=1 WHERE cabin_id=?",cabin);
  ExecutorService pool=Executors.newFixedThreadPool(12);try{
   List<Callable<Boolean>> work=new ArrayList<>();for(int i=0;i<50;i++)work.add(()->{try{hold(1);return true;}catch(ApiException e){assertEquals(409,e.getStatus());return false;}});
   long won=0;for(Future<Boolean> result:pool.invokeAll(work))if(result.get())won++;assertEquals(1,won);invariant();
  }finally{pool.shutdownNow();}
 }
 @Test void cancellationPaymentAndCleanupCompeteOnce()throws Exception{
  Long id=hold(1);ExecutorService pool=Executors.newFixedThreadPool(3);try{
   List<Callable<Void>> tasks=List.of(()->{try{booking.confirm(id,user);}catch(ApiException expected){}return null;},()->{try{booking.cancel(id,user);}catch(ApiException expected){}return null;},()->{booking.expire(id);return null;});
   for(Future<Void> f:pool.invokeAll(tasks))f.get();invariant();assertTrue(number("SELECT COUNT(*) FROM jpdd WHERE hold_id=?",id)<=1);
  }finally{pool.shutdownNow();}
 }
 @Test void refundRejectReapplyAndApproveAreIdempotent(){
  Long id=hold(2);booking.confirm(id,user);int order=number("SELECT ddid FROM jpdd WHERE hold_id=?",id);
  booking.requestRefund(order,user);int refund=number("SELECT id FROM tdjp WHERE ddid=?",order);
  booking.review(refund,false,"补充信息",admin);assertEquals(1,number("SELECT status FROM jpdd WHERE ddid=?",order));invariant();
  booking.requestRefund(order,user);booking.review(refund,true,"同意退订",admin);booking.review(refund,true,"同意退订",admin);invariant();
  assertEquals(10,number("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));
 }
 @Test void transactionFailureDoesNotConsumeInventory(){
  // A database uniqueness failure occurs after the conditional inventory debit.
  db.update("ALTER TABLE seat_hold ADD CONSTRAINT it_failure CHECK (userid<>"+user+")");
  try{assertThrows(RuntimeException.class,()->hold(1));assertEquals(10,number("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));}finally{db.update("ALTER TABLE seat_hold DROP CHECK it_failure");}
 }
 @Test void responsesNeverReturnPassword()throws Exception{
  String text=mvc.perform(get("/api/user/info").param("userId",Integer.toString(other)).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  JsonNode data=json.readTree(text).path("data");assertEquals(user,data.path("id").asInt());assertFalse(data.has("password"));
  String info=mvc.perform(get("/api/admin/info").header("Authorization",adminToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(json.readTree(info).path("data").has("password"));
 }
 @Test void httpHoldResponseAndAggregatedAvailability()throws Exception{
  String response=mvc.perform(post("/api/jpdd/hold").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("hbid",flight,"cabinId",cabin,"quantity",2,"userid",other,"requestId",UUID.randomUUID().toString())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  JsonNode data=json.readTree(response).path("data");assertTrue(data.path("holdId").asLong()>0);assertFalse(data.path("expireTime").isMissingNode());
  String search=mvc.perform(get("/api/hbxx/search").param("start","测试出发")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertEquals(8,json.readTree(search).path("data").path("records").get(0).path("number").asInt());
  mvc.perform(get("/api/jpdd/myHolds").header("Authorization",token)).andExpect(status().isOk());invariant();
 }
 @Test void cabinCapacityCannotDiscardBookedSeats(){
  hold(3);com.airticket.entity.HbxxCabin c=cabins.getById(cabin);c.setTotalNumber(2);
  assertThrows(IllegalArgumentException.class,()->cabins.update(c));c.setTotalNumber(12);c.setAvailableNumber(999);cabins.update(c);
  assertEquals(9,number("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));invariant();
 }
 @Test void singleSeatInitializationAndHistoryDeletionGuard(){
  com.airticket.entity.Hbxx f=new com.airticket.entity.Hbxx();f.setStart("单座出发");f.setEnd("单座到达");f.setStarttime(new java.util.Date(System.currentTimeMillis()+172800000L));f.setNumber(1);f.setPrice(java.math.BigDecimal.TEN);
  flights.add(f);
  try{assertEquals(1,number("SELECT SUM(total_number)FROM hbxx_cabin WHERE hbid=?",f.getHbid()));}finally{flights.delete(f.getHbid());}
  hold(1);assertThrows(IllegalArgumentException.class,()->flights.delete(flight));assertThrows(IllegalArgumentException.class,()->cabins.deleteById(cabin));
 }
 @Test void adminUpdateIsLimitedToRequestedRow(){
  int baseline=number("SELECT COUNT(*)FROM admin WHERE id<>? AND role=0",admin);
  com.airticket.entity.Admin a=admins.findById(admin);a.setUsername(tag+"new");a.setRole(1);a.setMenuId("4");admins.update(a);
  assertEquals(baseline,number("SELECT COUNT(*)FROM admin WHERE id<>? AND role=0",admin));assertEquals(tag+"new",admins.findById(admin).getUsername());
 }
 @Test void parallelRefundReviewRestoresOnlyOnce()throws Exception{
  Long id=hold(1);booking.confirm(id,user);int order=number("SELECT ddid FROM jpdd WHERE hold_id=?",id);booking.requestRefund(order,user);int refund=number("SELECT id FROM tdjp WHERE ddid=?",order);
  ExecutorService pool=Executors.newFixedThreadPool(2);try{Callable<Void> task=()->{booking.review(refund,true,"同意退订",admin);return null;};for(Future<Void> f:pool.invokeAll(List.of(task,task)))f.get();invariant();assertEquals(10,number("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));}finally{pool.shutdownNow();}
 }
 @Test void passwordChangeRevokesTokenAndInvalidPagesAre400()throws Exception{
  mvc.perform(put("/api/user/updatePassword").header("Authorization",token).contentType("application/json").content("{\"oldPassword\":\"test123456\",\"newPassword\":\"changed123456\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/user/info").header("Authorization",token)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/hbxx/search").param("pageSize","-1")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/hbxx/search").param("starttime","invalid")).andExpect(status().isBadRequest());
 }
 @Test void adminUsersArePaginatedAndSecretsExcluded()throws Exception{
  db.update("UPDATE admin SET role=0 WHERE id=?",admin);
  String response=mvc.perform(get("/api/admin/user/list").param("pageSize","1").header("Authorization",adminToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  JsonNode data=json.readTree(response).path("data");assertEquals(1,data.path("records").size());assertTrue(data.path("total").asInt()>=2);assertFalse(data.path("records").get(0).has("password"));
 }
}
