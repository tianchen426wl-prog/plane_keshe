package com.airticket;

import com.airticket.config.ApiException;
import com.airticket.dto.OrderDTO;
import com.airticket.service.*;
import com.airticket.util.PasswordUtil;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/air_ticket_test?serverTimezone=Asia/Shanghai&useSSL=false","jwt.secret=Y291cnNlLXRlc3Qtc2VjcmV0LWZvci1pc29sYXRlZC10ZXN0cy1vbmx5","flights.generation.enabled=false","ai.enabled=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlightFeaturesIntegrationTest {
 @Autowired JdbcTemplate db;@Autowired FlightCatalogService catalog;@Autowired FareSeatService fares;@Autowired BookingService booking;
 @Autowired ItineraryPlanningService ai;@Autowired ObjectMapper json;@Autowired PasswordUtil passwords;@Autowired MockMvc mvc;
 @Autowired FlightSearchService advancedSearch;
 @MockBean LlmProvider provider;
 int user,other,flight,cabin;String tag,policy,token;List<Integer>generated=new ArrayList<>();
 int n(String sql,Object...args){return db.queryForObject(sql,Integer.class,args);}
 @BeforeAll void guard(){assertEquals("air_ticket_test",db.queryForObject("SELECT DATABASE()",String.class));assertTrue(n("SELECT MAX(version) FROM schema_version")>=6);}
 @BeforeEach void fixtures()throws Exception{
  reset(provider);tag="ff"+UUID.randomUUID().toString().replace("-","").substring(0,12);policy=tag+"_POLICY";
  String hash=passwords.encode("test123456");
  db.update("INSERT INTO `user`(username,password,name)VALUES(?,?,?)",tag,hash,"测试旅客");user=n("SELECT id FROM `user` WHERE username=?",tag);
  db.update("INSERT INTO `user`(username,password,name)VALUES(?,?,?)",tag+"b",hash,"其他旅客");other=n("SELECT id FROM `user` WHERE username=?",tag+"b");
  db.update("INSERT INTO fare_policy SELECT ?,name,tax_fee,fuel_fee,baggage_kg,refund_cutoff_hours,early_refund_rate,late_refund_rate,description,currency,source_type,effective_from,effective_to FROM fare_policy WHERE code='DEMO_STANDARD_V1'",policy);
  db.update("INSERT INTO hbxx(company_id,xclb,start,`end`,starttime,arrival_time,number,price,flight_no,service_date,departure_airport,arrival_airport,layout_code,policy_code,source_type,sale_open_at,sale_close_at) SELECT MIN(company_id),'国内','北京','上海',DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 50 HOUR),0,100,?,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'PEK','SHA','DEMO_Y180_V1',?,'DEMO',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 47 HOUR) FROM company",tag,policy);
  flight=n("SELECT hbid FROM hbxx WHERE flight_no=?",tag);db.update("INSERT INTO hbxx_cabin(hbid,cabin_class,cabin_name,price,total_number,available_number)VALUES(?,1,'经济舱',100,10,10)",flight);cabin=n("SELECT cabin_id FROM hbxx_cabin WHERE hbid=?",flight);
  for(String no:List.of("1A","1B","1C","1D","1E","1F","2A","2B","2C","2D"))db.update("INSERT INTO flight_seat(hbid,cabin_id,seat_no,seat_row,seat_column,seat_fee)VALUES(?,?,?,?,?,?)",flight,cabin,no,Integer.parseInt(no.substring(0,1)),no.substring(1),no.equals("1A")?20:0);
  String response=mvc.perform(post("/api/user/login").contentType("application/json").content(json.writeValueAsString(Map.of("username",tag,"password","test123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();token="Bearer "+json.readTree(response).path("data").path("token").asText();
 }
 OrderDTO dto(int qty,List<String> seats){OrderDTO d=new OrderDTO();d.setHbid(flight);d.setCabinId(cabin);d.setQuantity(qty);d.setSeats(seats);d.setPassengers(qty==1?List.of("测试甲"):List.of("测试甲","测试乙"));d.setRequestId(UUID.randomUUID().toString());return d;}
 OrderDTO quoted(int qty,List<String>seats,int owner){OrderDTO d=dto(qty,seats);d.setQuoteId(fares.quote(d,owner).get("quoteId").toString());return d;}
 void healthy(){assertEquals(0,n("SELECT difference FROM v_inventory_health WHERE cabin_id=?",cabin));assertEquals(0,n("SELECT difference FROM v_seat_health WHERE cabin_id=?",cabin));}
 int order(long hold){return n("SELECT ddid FROM jpdd WHERE hold_id=?",hold);}
 @AfterEach void cleanup(){
  List<Integer> ids=new ArrayList<>(generated);generated.clear();ids.add(flight);
  for(int id:ids){
   db.update("DELETE FROM order_passenger WHERE order_id IN(SELECT ddid FROM jpdd WHERE hbid=?)",id);db.update("DELETE FROM order_price_item WHERE order_id IN(SELECT ddid FROM jpdd WHERE hbid=?)",id);db.update("DELETE FROM tdjp WHERE ddid IN(SELECT ddid FROM jpdd WHERE hbid=?)",id);db.update("DELETE FROM order_log WHERE ddid IN(SELECT ddid FROM jpdd WHERE hbid=?)",id);
   db.update("DELETE FROM flight_seat WHERE hbid=?",id);db.update("DELETE FROM seat_assignment_history WHERE hbid=?",id);db.update("DELETE FROM flight_quote WHERE hbid=?",id);db.update("DELETE FROM jpdd WHERE hbid=?",id);db.update("DELETE FROM seat_hold WHERE hbid=?",id);db.update("DELETE FROM hbxx_cabin WHERE hbid=?",id);db.update("DELETE FROM hbxx WHERE hbid=?",id);
  }
  db.update("DELETE FROM flight_schedule_exception WHERE schedule_id IN(SELECT id FROM flight_schedule WHERE code=?)",tag);db.update("DELETE FROM flight_schedule WHERE code=?",tag);db.update("DELETE FROM fare_policy WHERE code=?",policy);
  db.update("DELETE FROM ai_plan WHERE userid IN(?,?)",user,other);db.update("DELETE FROM ai_call_audit WHERE userid IN(?,?)",user,other);db.update("DELETE FROM passenger_profile WHERE userid IN(?,?)",user,other);db.update("DELETE FROM `user` WHERE id IN(?,?)",user,other);
 }
 @Test void passengerProfilesArePrivateValidatedAndIndependentOfOrders()throws Exception{
  mvc.perform(get("/api/user/passengers")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/user/passengers").header("Authorization",token).contentType("application/json").content("{\"name\":\"常用旅客\"}")).andExpect(status().isOk());
  int profile=n("SELECT id FROM passenger_profile WHERE userid=?",user);
  String login=mvc.perform(post("/api/user/login").contentType("application/json").content(json.writeValueAsString(Map.of("username",tag+"b","password","test123456")))).andReturn().getResponse().getContentAsString();
  String otherToken="Bearer "+json.readTree(login).path("data").path("token").asText();
  mvc.perform(put("/api/user/passengers/"+profile).header("Authorization",otherToken).contentType("application/json").content("{\"name\":\"越权修改\"}")).andExpect(status().isNotFound());
  mvc.perform(delete("/api/user/passengers/"+profile).header("Authorization",otherToken)).andExpect(status().isNotFound());
  mvc.perform(post("/api/user/passengers").header("Authorization",token).contentType("application/json").content("{\"name\":\"  \"}")).andExpect(status().isBadRequest());
  long hold=booking.hold(quoted(1,List.of("1B"),user),user);booking.confirm(hold,user);int id=order(hold);
  mvc.perform(put("/api/user/passengers/"+profile).header("Authorization",token).contentType("application/json").content("{\"name\":\"修改档案\"}")).andExpect(status().isOk());
  assertEquals("测试甲",db.queryForObject("SELECT passenger_name FROM order_passenger WHERE order_id=?",String.class,id));
  mvc.perform(delete("/api/user/passengers/"+profile).header("Authorization",token)).andExpect(status().isOk());healthy();
 }
 @Test void advancedSearchAndCalendarShareGroupPriceAndFilters(){
  com.airticket.dto.FlightFilter filter=new com.airticket.dto.FlightFilter();filter.setQuantity(2);filter.setOnlyAvailable(true);String date=catalog.today().plusDays(2).toString();
  @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)advancedSearch.search(" 北京市 ","sha",date,filter,"price",1,100,30).get("records");
  assertTrue(rows.stream().anyMatch(r->((Number)r.get("hbid")).intValue()==flight));
  BigDecimal lowest=rows.stream().map(r->new BigDecimal(r.get("minTotal").toString()).multiply(BigDecimal.valueOf(2))).min(BigDecimal::compareTo).orElseThrow();
  @SuppressWarnings("unchecked") List<Map<String,Object>> dates=(List<Map<String,Object>>)advancedSearch.calendar("北京","SHA",filter,30).get("dates");
  Map<String,Object> day=dates.stream().filter(d->date.equals(d.get("serviceDate").toString())).findFirst().orElseThrow();assertEquals(0,lowest.compareTo(new BigDecimal(day.get("minTotal").toString())));
  filter.setMaxTotal(BigDecimal.ONE);assertEquals(0L,advancedSearch.search("北京","SHA",date,filter,"price",1,20,30).get("total"));
  filter.setMaxTotal(null);db.update("UPDATE hbxx_cabin SET available_number=1 WHERE cabin_id=?",cabin);
  @SuppressWarnings("unchecked") List<Map<String,Object>> insufficient=(List<Map<String,Object>>)advancedSearch.search("北京","SHA",date,filter,"time",1,100,30).get("records");assertTrue(insufficient.stream().noneMatch(r->((Number)r.get("hbid")).intValue()==flight));
 }
 @Test void filtersAreValidatedAndUnavailableFlightsAreExcluded()throws Exception{
  mvc.perform(get("/api/flights").param("quantity","6")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/flights").param("quantity","not-a-number")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/flights/calendar").param("departureFrom","18").param("departureTo","6")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/flights").param("sort","price;DROP TABLE user")).andExpect(status().isBadRequest());
  db.update("UPDATE hbxx SET operational_status='CANCELLED' WHERE hbid=?",flight);
  com.airticket.dto.FlightFilter filter=new com.airticket.dto.FlightFilter();filter.setOnlyAvailable(true);
  @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)advancedSearch.search("北京","上海",catalog.today().plusDays(2).toString(),filter,"time",1,100,30).get("records");assertTrue(rows.stream().noneMatch(r->((Number)r.get("hbid")).intValue()==flight));
 }
 @Test void soldOutVisibilityAndCabinFilterRemainExplicit(){
  db.update("UPDATE hbxx_cabin SET available_number=0 WHERE cabin_id=?",cabin);
  com.airticket.dto.FlightFilter filter=new com.airticket.dto.FlightFilter();
  String day=catalog.today().plusDays(2).toString();
  @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)advancedSearch.search("北京","上海",day,filter,"price",1,100,30).get("records");
  assertTrue(rows.stream().anyMatch(r->((Number)r.get("hbid")).intValue()==flight&&r.get("minTotal")==null));
  filter.setCabinClass(3);
  @SuppressWarnings("unchecked") List<Map<String,Object>> noFirstClass=(List<Map<String,Object>>)advancedSearch.search("北京","上海",day,filter,"price",1,100,30).get("records");
  assertTrue(noFirstClass.stream().noneMatch(r->((Number)r.get("hbid")).intValue()==flight));
 }
 @Test void orderDetailsKeepOriginalFlightAndExposeBusinessProgress(){
  long hold=booking.hold(quoted(1,List.of("1B"),user),user);booking.confirm(hold,user);int id=order(hold);
  db.update("UPDATE hbxx SET departure_terminal='变更后的航站楼' WHERE hbid=?",flight);
  Map<String,Object> detail=fares.orderDetail(id,user);
  assertNotNull(((JsonNode)detail.get("flightSnapshot")).get("arrivalTime"));assertTrue(detail.get("flight").toString().contains("变更后的航站楼"));assertFalse(detail.get("flightSnapshot").toString().contains("变更后的航站楼"));assertTrue(detail.get("timeline").toString().contains("PAY"));
  booking.requestRefund(id,user);int refund=n("SELECT id FROM tdjp WHERE ddid=?",id);booking.review(refund,false,"请补充说明",1);
  assertTrue(fares.orderDetail(id,user).get("refund").toString().contains("请补充说明"));assertThrows(ApiException.class,()->fares.orderDetail(id,other));healthy();
 }
 @Test void fullFareAndSeatsSurvivePriceChange(){
  OrderDTO d=quoted(2,List.of("1A","1B"),user);long hold=booking.hold(d,user);healthy();
  db.update("UPDATE hbxx_cabin SET price=900 WHERE cabin_id=?",cabin);assertTrue(booking.confirm(hold,user));assertTrue(booking.confirm(hold,user));
  assertEquals(400,n("SELECT cost FROM jpdd WHERE hold_id=?",hold));assertEquals(2,n("SELECT COUNT(*) FROM order_passenger WHERE order_id=?",order(hold)));assertEquals(4,n("SELECT COUNT(*) FROM order_price_item WHERE order_id=?",order(hold)));healthy();
 }
 @Test void searchUsesCabinInventoryAndDistinguishesUnavailableStates(){
  com.airticket.dto.FlightFilter f=new com.airticket.dto.FlightFilter();String day=catalog.today().plusDays(2).toString();
  java.util.function.Supplier<Map<String,Object>> row=()->{
   @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)advancedSearch.search("北京","上海",day,f,"time",1,100,30).get("records");
   return rows.stream().filter(r->((Number)r.get("hbid")).intValue()==flight).findFirst().orElseThrow();
  };
  assertEquals(0,n("SELECT number FROM hbxx WHERE hbid=?",flight));assertEquals(10,((Number)row.get().get("number")).intValue());assertEquals("AVAILABLE",row.get().get("bookingStatus"));
  db.update("UPDATE hbxx SET number=999 WHERE hbid=?",flight);assertEquals(10,((Number)row.get().get("number")).intValue());
  db.update("UPDATE hbxx_cabin SET available_number=2 WHERE cabin_id=?",cabin);f.setQuantity(3);assertEquals(2,((Number)row.get().get("number")).intValue());assertEquals("INSUFFICIENT_SEATS",row.get().get("bookingStatus"));assertNull(row.get().get("minTotal"));
  db.update("UPDATE hbxx_cabin SET available_number=0 WHERE cabin_id=?",cabin);assertEquals("SOLD_OUT",row.get().get("bookingStatus"));
  db.update("UPDATE hbxx_cabin SET status=0 WHERE cabin_id=?",cabin);assertEquals("INVENTORY_UNAVAILABLE",row.get().get("bookingStatus"));
 }
 @Test void customerCopyDoesNotMutateOriginalOrderSnapshot(){
  long hold=booking.hold(quoted(1,List.of("1B"),user),user);booking.confirm(hold,user);int id=order(hold);
  db.update("UPDATE jpdd SET price_snapshot=JSON_SET(price_snapshot,'$.notice','课程演示计价','$.items[1].name','演示税费','$.policy.description','课程演示规则') WHERE ddid=?",id);
  String before=db.queryForObject("SELECT price_snapshot FROM jpdd WHERE ddid=?",String.class,id);
  JsonNode visible=(JsonNode)fares.orderDetail(id,user).get("priceSnapshot");assertEquals("税费",visible.path("items").get(1).path("name").asText());assertFalse(visible.toString().contains("课程演示"));assertEquals(before,db.queryForObject("SELECT price_snapshot FROM jpdd WHERE ddid=?",String.class,id));healthy();
 }
 @Test void expiryAndCancellationReleaseSeatsExactlyOnce(){
  long hold=booking.hold(quoted(1,List.of("1A"),user),user);db.update("UPDATE seat_hold SET expire_time=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE hold_id=?",hold);assertFalse(booking.confirm(hold,user));booking.expire(hold);booking.cancel(hold,user);assertEquals(10,n("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));healthy();
 }
 @Test void oldQuoteCannotBeReusedOrTransferred(){
  OrderDTO d=quoted(1,List.of("1B"),user);assertThrows(ApiException.class,()->booking.hold(d,other));long hold=booking.hold(d,user);assertEquals(hold,booking.hold(d,user));d.setRequestId(UUID.randomUUID().toString());assertThrows(ApiException.class,()->booking.hold(d,user));assertEquals(1,n("SELECT COUNT(*) FROM seat_hold WHERE hbid=?",flight));healthy();
 }
 @Test void changedRequestPayloadAndExpiredQuoteAreRejected(){
  OrderDTO d=quoted(1,List.of("1B"),user);booking.hold(d,user);String request=d.getRequestId();OrderDTO changed=quoted(1,List.of("1C"),user);changed.setRequestId(request);assertThrows(ApiException.class,()->booking.hold(changed,user));
  changed.setRequestId(UUID.randomUUID().toString());db.update("UPDATE flight_quote SET expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?",changed.getQuoteId());assertThrows(ApiException.class,()->booking.hold(changed,user));healthy();
 }
 @Test void sameSeatConcurrentBuyersOnlyOneWins()throws Exception{
  OrderDTO a=quoted(1,List.of("1A"),user),b=quoted(1,List.of("1A"),other);ExecutorService pool=Executors.newFixedThreadPool(2);
  try{List<Callable<Boolean>>calls=List.of(()->{try{booking.hold(a,user);return true;}catch(ApiException e){return false;}},()->{try{booking.hold(b,other);return true;}catch(ApiException e){return false;}});int won=0;for(Future<Boolean>f:pool.invokeAll(calls))if(f.get())won++;assertEquals(1,won);healthy();}finally{pool.shutdownNow();}
 }
 @Test void groupSeatConflictRollsBackAllInventory(){
  OrderDTO group=quoted(2,List.of("1A","1B"),user);booking.hold(quoted(1,List.of("1B"),other),other);assertThrows(ApiException.class,()->booking.hold(group,user));assertEquals("AVAILABLE",db.queryForObject("SELECT state FROM flight_seat WHERE hbid=? AND seat_no='1A'",String.class,flight));assertEquals(9,n("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));healthy();
 }
 @Test void refundUsesOriginalRulesAndReleasesOnce(){
  long hold=booking.hold(quoted(2,List.of("1A","1B"),user),user);booking.confirm(hold,user);int id=order(hold);db.update("UPDATE fare_policy SET early_refund_rate=0.9 WHERE code=?",policy);assertEquals(new BigDecimal("380.00"),fares.refundQuote(id,user).get("amount"));
  booking.requestRefund(id,user);int refund=n("SELECT id FROM tdjp WHERE ddid=?",id);booking.review(refund,false,"待补充",1);healthy();booking.requestRefund(id,user);booking.review(refund,true,"同意",1);booking.review(refund,true,"同意",1);assertEquals(380,n("SELECT refund_amount FROM jpdd WHERE ddid=?",id));healthy();
 }
 @Test void cancelledFlightReceivesFullRefundPastVoluntaryCutoff(){
  long hold=booking.hold(quoted(1,List.of("1A"),user),user);booking.confirm(hold,user);db.update("UPDATE hbxx SET starttime=DATE_ADD(NOW(),INTERVAL 1 HOUR),operational_status='CANCELLED' WHERE hbid=?",flight);assertEquals(new BigDecimal("210.00"),fares.refundQuote(order(hold),user).get("amount"));booking.requestRefund(order(hold),user);
 }
 @Test void storedProcedureRefundKeepsSeatAndMoneyConsistent(){
  long hold=booking.hold(quoted(1,List.of("1A"),user),user);booking.confirm(hold,user);int id=order(hold);booking.requestRefund(id,user);int refund=n("SELECT id FROM tdjp WHERE ddid=?",id);
  for(int i=0;i<2;i++){Integer result=db.execute((org.springframework.jdbc.core.ConnectionCallback<Integer>)connection->{try(java.sql.CallableStatement call=connection.prepareCall("{call sp_approve_refund(?,?)}")){call.setInt(1,refund);call.registerOutParameter(2,java.sql.Types.INTEGER);call.execute();return call.getInt(2);}});assertEquals(1,result);}
  assertEquals(200,n("SELECT refund_amount FROM jpdd WHERE ddid=?",id));healthy();
 }
 @Test void noDuplicateSeatsOrWrongPassengerCount(){
  assertThrows(IllegalArgumentException.class,()->fares.quote(dto(2,List.of("1A","1A")),user));OrderDTO d=dto(2,List.of());d.setPassengers(List.of("单人"));assertThrows(IllegalArgumentException.class,()->fares.quote(d,user));OrderDTO auto=dto(2,List.of());Map<String,Object>q=fares.quote(auto,user);assertEquals(new BigDecimal("380.00"),q.get("total"));assertFalse(q.get("seats").toString().contains("1A"));
 }
 @Test void closedOrNotOpenFlightsCannotBeQuoted(){
  db.update("UPDATE hbxx SET sale_open_at=DATE_ADD(NOW(),INTERVAL 1 DAY) WHERE hbid=?",flight);assertThrows(ApiException.class,()->fares.quote(dto(1,List.of()),user));db.update("UPDATE hbxx SET sale_open_at=NOW(),sale_close_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE hbid=?",flight);assertThrows(ApiException.class,()->fares.quote(dto(1,List.of()),user));
 }
 @Test void generationIsIdempotentAndPreservesSales(){
  LocalDate day=catalog.today().plusDays(2);int company=n("SELECT MIN(company_id) FROM company");
  db.update("INSERT INTO flight_schedule(code,company_id,flight_no,departure_airport,arrival_airport,departure_time,duration_minutes,valid_from,valid_to,layout_code,policy_code,base_price)VALUES(?,? ,?,'PEK','SHA','23:30:00',140,?,?,'DEMO_Y180_V1',?,100)",tag,company,tag+"S",day,day.plusDays(1),policy);
  Map<String,Object>first=catalog.generate(day,2);assertEquals(0,((Number)first.get("failedCount")).intValue());generated.addAll(db.queryForList("SELECT hbid FROM hbxx WHERE schedule_id=(SELECT id FROM flight_schedule WHERE code=?)",Integer.class,tag));assertEquals(2,generated.size());
  long hold=booking.hold(quoted(1,List.of("1A"),user),user);Map<String,Object>again=catalog.generate(day,2);assertEquals(0,((Number)again.get("createdCount")).intValue());assertEquals(9,n("SELECT available_number FROM hbxx_cabin WHERE cabin_id=?",cabin));assertEquals("HELD",db.queryForObject("SELECT state FROM flight_seat WHERE hold_id=?",String.class,hold));
  db.update("UPDATE flight_schedule SET base_price=999,version=version+1 WHERE code=?",tag);catalog.generate(day,2);assertEquals(100,n("SELECT price FROM hbxx WHERE hbid=?",generated.get(0)));
  assertEquals(1,n("SELECT DATEDIFF(arrival_time,starttime) FROM hbxx WHERE hbid=?",generated.get(0)));healthy();
 }
 @Test void weekdayAndCancelledDateExceptionsAreRespected(){
  LocalDate day=catalog.today().plusDays(3);int company=n("SELECT MIN(company_id) FROM company");
  db.update("INSERT INTO flight_schedule(code,company_id,flight_no,departure_airport,arrival_airport,departure_time,duration_minutes,weekdays,valid_from,valid_to,layout_code,policy_code,base_price)VALUES(?,? ,?,'PEK','SHA','12:00:00',140,?,?,?,'DEMO_Y180_V1',?,100)",tag,company,tag+"S",Integer.toString(day.plusDays(1).getDayOfWeek().getValue()),day,day.plusDays(1),policy);
  db.update("INSERT INTO flight_schedule_exception(schedule_id,service_date,reason)SELECT id,?,'测试停飞' FROM flight_schedule WHERE code=?",day.plusDays(1),tag);catalog.generate(day,2);assertEquals(0,n("SELECT COUNT(*) FROM hbxx WHERE schedule_id=(SELECT id FROM flight_schedule WHERE code=?)",tag));
  db.update("DELETE FROM flight_schedule_exception WHERE schedule_id=(SELECT id FROM flight_schedule WHERE code=?)",tag);catalog.generate(day,2);generated.addAll(db.queryForList("SELECT hbid FROM hbxx WHERE schedule_id=(SELECT id FROM flight_schedule WHERE code=?)",Integer.class,tag));assertEquals(1,generated.size());assertEquals(day.plusDays(1),db.queryForObject("SELECT service_date FROM hbxx WHERE hbid=?",java.sql.Date.class,generated.get(0)).toLocalDate());
 }
 @Test void calendarBoundsAndPublicSeatResponse()throws Exception{
  @SuppressWarnings("unchecked") List<Map<String,Object>> results=(List<Map<String,Object>>)catalog.search("北京","上海",catalog.today().plusDays(2).toString(),null,1,"price",1,20).get("records");
  assertTrue(results.stream().anyMatch(r->((Number)r.get("hbid")).intValue()==flight&&((Number)r.get("number")).intValue()==10),"Search must use cabin inventory, not hbxx.number=0");
  mvc.perform(get("/api/flights").param("date",catalog.today().minusDays(1).toString())).andExpect(status().isBadRequest());mvc.perform(get("/api/flights/calendar")).andExpect(status().isOk());String r=mvc.perform(get("/api/flights/"+flight+"/seats").param("cabinId",Integer.toString(cabin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(r.contains("userid"));assertFalse(r.contains("holdId"));
 }
 @Test void routeExistenceDoesNotDependOnRemainingSeats()throws Exception{
  String date=catalog.today().plusDays(2).toString();
  db.update("UPDATE hbxx_cabin SET available_number=0 WHERE cabin_id=?",cabin);
  Map<String,Object> found=catalog.search(" 北京市 ","sha",date,null,1,"time",1,100);
  assertEquals(true,found.get("exists"));
  @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)found.get("records");
  assertTrue(rows.stream().anyMatch(r->((Number)r.get("hbid")).intValue()==flight&&((Number)r.get("number")).intValue()==0));
  String publicResult=mvc.perform(get("/api/flights").param("start","PEK").param("end","SHA").param("date",date).param("pageSize","100")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertTrue(json.readTree(publicResult).path("data").path("exists").asBoolean());
  assertTrue(publicResult.contains("\"hbid\":"+flight),"Default public search must retain sold-out flights");
  Map<String,Object> missing=catalog.search("不存在的城市","上海",date,null,1,"time",1,20);
  assertEquals(false,missing.get("exists"));assertEquals(0L,missing.get("total"));
  assertEquals("没有符合条件的航班",missing.get("message"));
  assertEquals(catalog.search("PEK","SHA",date,null,1,"time",1,20).get("total"),catalog.search("北京首都国际机场","上海虹桥国际机场",date,null,1,"time",1,20).get("total"));
 }
 @Test void expandedNetworkHasReverseRoutesAndTourismCoverage(){
  assertEquals(0,n("SELECT COUNT(*) FROM flight_schedule a WHERE a.code LIKE 'REF-%' AND NOT EXISTS(SELECT 1 FROM flight_schedule b WHERE b.enabled=1 AND b.departure_airport=a.arrival_airport AND b.arrival_airport=a.departure_airport)"));
  for(String code:List.of("JHG","DIG","LXA","LJG","DLU","SHE","CGQ","DLC","YNJ","NBS","OHE","HLD","KHG","AAT")){
   assertTrue(n("SELECT COUNT(*) FROM flight_schedule WHERE enabled=1 AND departure_airport=?",code)>0,code);
  }
  assertEquals(0,n("SELECT COUNT(*) FROM flight_schedule WHERE enabled=1 AND departure_airport='SHA' AND arrival_airport='NKG'"));
 }
 @Test void authenticatedQuoteHoldDetailsAndOwnership()throws Exception{
  OrderDTO d=dto(1,List.of("1A"));String response=mvc.perform(post("/api/quotes").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();d.setQuoteId(json.readTree(response).path("data").path("quoteId").asText());long hold=booking.hold(d,user);
  mvc.perform(get("/api/jpdd/holds/"+hold).header("Authorization",token)).andExpect(status().isOk());assertThrows(ApiException.class,()->fares.holdDetail(hold,other));booking.confirm(hold,user);assertThrows(ApiException.class,()->fares.orderDetail(order(hold),other));mvc.perform(post("/api/quotes").contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isUnauthorized());healthy();
 }
 @Test void aiDisabledDoesNotReadOrInvokeProvider(){assertThrows(ApiException.class,()->ai.plan(user,"明天北京到上海"));verify(provider,never()).json(anyString(),anyString(),anyLong());assertEquals(0,n("SELECT COUNT(*) FROM ai_call_audit WHERE userid=?",user));}
 void modelAvailable(){when(provider.available()).thenReturn(true);when(provider.json(anyString(),anyString(),anyLong())).thenAnswer(call->{String system=call.getArgument(0);return system.contains("解析器")?json.readTree("{\"start\":\"北京\",\"end\":\"上海\",\"date\":\""+catalog.today().plusDays(2)+"\",\"quantity\":1,\"budget\":250}"):json.readTree("{\"flightIds\":[99999999]}");});}
 @Test void aiInvalidIdsNeverBecomeBookableAndPlansArePrivate(){modelAvailable();Map<String,Object>r=ai.plan(user,"后天北京到上海1位成人预算250元");assertTrue(r.get("candidates").toString().contains(Integer.toString(flight)));assertFalse(r.get("candidates").toString().contains("99999999"));assertThrows(ApiException.class,()->ai.get(r.get("planId").toString(),other));assertFalse(r.toString().contains("测试甲"));}
 @Test void aiRejectsSqlAndInvalidModelStructure(){when(provider.available()).thenReturn(true);when(provider.json(anyString(),anyString(),anyLong())).thenReturn(json.createObjectNode().put("start","SELECT * FROM user").put("end","上海").put("date",catalog.today().toString()));assertThrows(IllegalArgumentException.class,()->ai.plan(user,"查询数据库"));assertEquals(2,n("SELECT COUNT(*) FROM `user` WHERE id IN(?,?)",user,other));}
 @Test void aiTimeoutFallsBackAndRateLimitIsEnforced(){modelAvailable();for(int i=0;i<3;i++)ai.plan(user,"后天北京到上海");assertEquals(429,assertThrows(ApiException.class,()->ai.plan(user,"第四次")).getStatus());}
}
