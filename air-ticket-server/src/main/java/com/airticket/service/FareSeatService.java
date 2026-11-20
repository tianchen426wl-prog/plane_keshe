package com.airticket.service;

import com.airticket.config.ApiException;
import com.airticket.dto.OrderDTO;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

@Service
public class FareSeatService {
 @Autowired private JdbcTemplate db;
 @Autowired private ObjectMapper json;
 @Autowired private FlightCatalogService catalog;
 public String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("无法保存费用快照");}}
 public JsonNode decode(Object value){try{return json.readTree(value.toString());}catch(Exception e){throw new IllegalStateException("费用快照损坏");}}
 private BigDecimal money(Object value){return new BigDecimal(value.toString()).setScale(2,RoundingMode.HALF_UP);}
 public void sale(Map<String,Object>f){
  long now=System.currentTimeMillis();
  if(((Number)f.get("sale_status")).intValue()!=1||!"SCHEDULED".equals(f.get("operational_status"))||FlightCatalogService.epoch(f.get("starttime"))<=now)throw new ApiException(409,"航班已停售、取消或起飞");
  if(f.get("sale_open_at")!=null&&FlightCatalogService.epoch(f.get("sale_open_at"))>now)throw new ApiException(409,"航班尚未开售");
  if(f.get("sale_close_at")!=null&&FlightCatalogService.epoch(f.get("sale_close_at"))<=now)throw new ApiException(409,"航班已停止接受预订");
 }
 public Map<String,Object> seats(int flight,int cabin){
  catalog.required("SELECT hbid FROM hbxx WHERE hbid=?",flight);
  return Map.of("flightId",flight,"cabinId",cabin,"layoutNotice","请选择座位并核对费用", "seats",db.queryForList("SELECT seat_no,seat_row,seat_column,seat_fee,state FROM flight_seat WHERE hbid=? AND cabin_id=? ORDER BY seat_row,seat_column",flight,cabin).stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));
 }
 @Transactional
 public Map<String,Object> quote(OrderDTO dto,int user){
  if(dto.getHbid()==null||dto.getCabinId()==null||dto.getQuantity()==null||dto.getQuantity()<1||dto.getQuantity()>5)throw new IllegalArgumentException("请选择航班舱位和1～5位成人旅客");
  Map<String,Object> f=catalog.required("SELECT * FROM hbxx WHERE hbid=?",dto.getHbid());sale(f);
  if(f.get("layout_code")==null)throw new ApiException(409,"历史航班请使用原有购票流程");
  Map<String,Object> c=catalog.required("SELECT * FROM hbxx_cabin WHERE cabin_id=? AND hbid=?",dto.getCabinId(),dto.getHbid());
  if(((Number)c.get("status")).intValue()!=1||((Number)c.get("available_number")).intValue()<dto.getQuantity())throw new ApiException(409,"所选舱位余票不足");
  if(dto.getPassengers()==null||dto.getPassengers().size()!=dto.getQuantity())throw new IllegalArgumentException("请为每张票填写乘机人姓名");
  for(String name:dto.getPassengers())if(name==null||name.isBlank()||name.trim().length()>50||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("乘机人姓名应为1～50字");
  Map<String,Object>p=catalog.required("SELECT * FROM fare_policy WHERE code=? AND ? BETWEEN effective_from AND effective_to",f.get("policy_code"),f.get("service_date"));
  List<String> requested=dto.getSeats()==null?List.of():dto.getSeats();
  if(!requested.isEmpty()&&(requested.size()!=dto.getQuantity()||new HashSet<>(requested).size()!=requested.size()))throw new IllegalArgumentException("所选座位数必须与人数一致且不能重复");
  List<Map<String,Object>> seatRows=db.queryForList("SELECT * FROM flight_seat WHERE hbid=? AND cabin_id=? AND state='AVAILABLE' ORDER BY seat_row,seat_column",dto.getHbid(),dto.getCabinId());
  List<Map<String,Object>> selected=new ArrayList<>();
  if(requested.isEmpty()){
   List<Map<String,Object>> free=seatRows.stream().filter(s->money(s.get("seat_fee")).signum()==0).collect(java.util.stream.Collectors.toList());
   // Prefer one row; never promise adjacency across an aisle.
   Map<Object,List<Map<String,Object>>> groups=new LinkedHashMap<>();for(Map<String,Object>s:free)groups.computeIfAbsent(s.get("seat_row"),k->new ArrayList<>()).add(s);
   for(List<Map<String,Object>> row:groups.values())if(row.size()>=dto.getQuantity()){selected.addAll(row.subList(0,dto.getQuantity()));break;}
   if(selected.isEmpty()&&free.size()>=dto.getQuantity())selected.addAll(free.subList(0,dto.getQuantity()));
   if(selected.size()!=dto.getQuantity())throw new ApiException(409,"免费自动分配座位不足，请手动选择并核对费用");
  }else{for(String no:requested){Map<String,Object> found=seatRows.stream().filter(s->s.get("seat_no").equals(no)).findFirst().orElseThrow(()->new ApiException(409,"座位已被占用或不属于所选客舱，请重新选择"));selected.add(found);}}
  BigDecimal qty=BigDecimal.valueOf(dto.getQuantity());
  List<Map<String,Object>>items=new ArrayList<>();
  items.add(Map.of("code","BASE","name","基础运价","amount",money(c.get("price")).multiply(qty),"refundable",false));
  items.add(Map.of("code","TAX","name","税费","amount",money(p.get("tax_fee")).multiply(qty),"refundable",true));
  items.add(Map.of("code","FUEL","name","燃油附加费","amount",money(p.get("fuel_fee")).multiply(qty),"refundable",true));
  BigDecimal seatFee=selected.stream().map(s->money(s.get("seat_fee"))).reduce(BigDecimal.ZERO,BigDecimal::add);
  items.add(Map.of("code","SEAT","name","自选座位服务","amount",seatFee,"refundable",true));
  BigDecimal total=items.stream().map(i->money(i.get("amount"))).reduce(BigDecimal.ZERO,BigDecimal::add);
  Map<String,Object>snap=new LinkedHashMap<>();snap.put("currency","CNY");snap.put("sourceType","DEMO");snap.put("notice","费用以订单明细为准");snap.put("items",items);snap.put("total",total);snap.put("quantity",dto.getQuantity());snap.put("baseUnitPrice",c.get("price"));
  snap.put("seats",selected.stream().map(s->s.get("seat_no")).collect(java.util.stream.Collectors.toList()));snap.put("passengers",dto.getPassengers().stream().map(String::trim).collect(java.util.stream.Collectors.toList()));
  snap.put("policy",FlightCatalogService.camel(p));snap.put("adjacencyNotice","自动分配优先同排；座位可能分散，以所列座位号为准");
  String id=UUID.randomUUID().toString();db.update("INSERT INTO flight_quote(id,userid,hbid,cabin_id,quantity,total,snapshot,expires_at)VALUES(?,?,?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 5 MINUTE))",id,user,dto.getHbid(),dto.getCabinId(),dto.getQuantity(),total,encode(snap));
  Map<String,Object>result=new LinkedHashMap<>(snap);result.put("quoteId",id);result.put("expiresAt",db.queryForObject("SELECT expires_at FROM flight_quote WHERE id=?",java.sql.Timestamp.class,id));return result;
 }
 public void attach(long hold,OrderDTO dto,int user){
  Map<String,Object> f=catalog.required("SELECT * FROM hbxx WHERE hbid=?",dto.getHbid());sale(f);
  if(f.get("layout_code")==null)return;
  if(dto.getQuoteId()==null)throw new IllegalArgumentException("请先获取完整费用报价再锁定座位");
  Map<String,Object>q=catalog.required("SELECT * FROM flight_quote WHERE id=? FOR UPDATE",dto.getQuoteId());
  if(((Number)q.get("userid")).intValue()!=user||!q.get("hbid").equals(dto.getHbid())||!q.get("cabin_id").equals(dto.getCabinId())||!q.get("quantity").equals(dto.getQuantity()))throw new ApiException(409,"报价不属于本次预订");
  if(q.get("used_hold_id")!=null||FlightCatalogService.epoch(q.get("expires_at"))<=System.currentTimeMillis())throw new ApiException(409,"报价已使用或过期，请重新报价");
  JsonNode snapshot=decode(q.get("snapshot"));List<String>nos=new ArrayList<>();snapshot.path("seats").forEach(n->nos.add(n.asText()));Collections.sort(nos);
  for(String no:nos){if(db.update("UPDATE flight_seat SET state='HELD',hold_id=? WHERE hbid=? AND cabin_id=? AND seat_no=? AND state='AVAILABLE'",hold,dto.getHbid(),dto.getCabinId(),no)!=1)throw new ApiException(409,"座位被其他旅客选中，请刷新座位并重新报价");db.update("INSERT INTO seat_assignment_history(hbid,seat_no,hold_id,action)VALUES(?,?,?,'HOLD')",dto.getHbid(),no,hold);}
  db.update("UPDATE seat_hold SET quote_id=?,price_snapshot=?,total_cost=?,unit_price=? WHERE hold_id=?",dto.getQuoteId(),q.get("snapshot"),q.get("total"),snapshot.path("baseUnitPrice").decimalValue(),hold);
  db.update("UPDATE flight_quote SET used_hold_id=? WHERE id=?",hold,dto.getQuoteId());
 }
 public void release(long hold){
  db.update("INSERT INTO seat_assignment_history(hbid,seat_no,hold_id,action) SELECT hbid,seat_no,hold_id,'RELEASE' FROM flight_seat WHERE hold_id=? AND state IN('HELD','SOLD')",hold);
  db.update("UPDATE flight_seat SET state='AVAILABLE',hold_id=NULL,order_id=NULL WHERE hold_id=? AND state IN('HELD','SOLD')",hold);
 }
 public void confirm(long hold,long order){
  Object raw=catalog.required("SELECT price_snapshot FROM seat_hold WHERE hold_id=?",hold).get("price_snapshot");if(raw==null)return;
  JsonNode snap=decode(raw);
  int n=db.update("UPDATE flight_seat SET state='SOLD',order_id=? WHERE hold_id=? AND state='HELD'",order,hold);
  if(n!=snap.path("quantity").asInt())throw new ApiException(409,"座位与占位数量不一致");
  for(int i=0;i<snap.path("quantity").asInt();i++)db.update("INSERT INTO order_passenger(order_id,passenger_index,passenger_name,seat_no)VALUES(?,?,?,?)",order,i,snap.path("passengers").get(i).asText(),snap.path("seats").get(i).asText());
  for(JsonNode item:snap.path("items"))db.update("INSERT INTO order_price_item(order_id,item_code,item_name,amount,refundable)VALUES(?,?,?,?,?)",order,item.path("code").asText(),item.path("name").asText(),item.path("amount").decimalValue(),item.path("refundable").asBoolean());
  db.update("UPDATE jpdd SET price_snapshot=?,pricing_mode='ITEMIZED' WHERE ddid=?",raw,order);
  db.update("INSERT INTO seat_assignment_history(hbid,seat_no,hold_id,action) SELECT hbid,seat_no,hold_id,'CONFIRM' FROM flight_seat WHERE hold_id=?",hold);
 }
 public Map<String,Object> refundQuote(int order,int user){
  Map<String,Object>o=catalog.required("SELECT o.*,h.starttime,h.operational_status FROM jpdd o JOIN hbxx h ON h.hbid=o.hbid WHERE o.ddid=?",order);
  if(((Number)o.get("userid")).intValue()!=user)throw new ApiException(403,"无权访问此订单");
  if(o.get("price_snapshot")==null)return Map.of("amount",o.get("cost"),"fee",BigDecimal.ZERO,"mode","LEGACY_UNSPLIT","notice","历史订单按原有退订规则处理");
  JsonNode s=decode(o.get("price_snapshot"));long left=FlightCatalogService.epoch(o.get("starttime"))-System.currentTimeMillis();boolean cancelled="CANCELLED".equals(o.get("operational_status"));
  if(!cancelled&&left<s.path("policy").path("refundCutoffHours").asInt(2)*3600000L)throw new ApiException(409,"已超过该产品的退订截止时间");
  BigDecimal rate=cancelled?BigDecimal.ZERO:s.path("policy").path(left>=24*3600000L?"earlyRefundRate":"lateRefundRate").decimalValue();
  BigDecimal base=BigDecimal.ZERO;for(JsonNode i:s.path("items"))if("BASE".equals(i.path("code").asText()))base=i.path("amount").decimalValue();
  BigDecimal fee=base.multiply(rate).setScale(2,RoundingMode.HALF_UP);
  return Map.of("amount",money(o.get("cost")).subtract(fee),"fee",fee,"mode",cancelled?"INVOLUNTARY":"VOLUNTARY","notice",cancelled?"航班取消，全额退还订单费用":"依据购票时规则：基础运价收取退订费，税费、附加费和选座费退还");
 }
 public Map<String,Object> holdDetail(long hold,int user){
  Map<String,Object>r=FlightCatalogService.camel(catalog.required("SELECT s.*,c.cabin_name,h.start,h.`end`,h.starttime,h.arrival_time,h.flight_no,h.place,co.company_name FROM seat_hold s JOIN hbxx h ON h.hbid=s.hbid JOIN hbxx_cabin c ON c.cabin_id=s.cabin_id JOIN company co ON co.company_id=h.company_id WHERE s.hold_id=? AND s.userid=?",hold,user));
  if(r.get("priceSnapshot")!=null)r.put("priceSnapshot",CustomerPresentation.snapshot(decode(r.get("priceSnapshot"))));r.put("totalCost",r.get("totalCost")!=null?r.get("totalCost"):money(r.get("unitPrice")).multiply(new BigDecimal(r.get("quantity").toString())));r.put("serverTime",new java.util.Date());return r;
 }
 public Map<String,Object> orderDetail(int order,int user){
  Map<String,Object>o=FlightCatalogService.camel(catalog.required("SELECT ddid,hbid,userid,quantity,ordertime,order_no,status,cost,pricing_mode,price_snapshot,flight_snapshot,refund_amount FROM jpdd WHERE ddid=? AND userid=?",order,user));
  o.put("flight",flightForSnapshot(((Number)o.get("hbid")).intValue()));
  o.put("flightSnapshot",o.get("flightSnapshot")==null?null:CustomerPresentation.snapshot(decode(o.get("flightSnapshot"))));
  o.put("timeline",db.queryForList("SELECT action,log_time,remark FROM order_log WHERE ddid=? AND action IN('CREATE','PAY','REFUND_REQUEST','REFUND_APPROVE','REFUND_REJECT') ORDER BY log_time,log_id",order).stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));
  for(Map<String,Object> event:(List<Map<String,Object>>)o.get("timeline"))if("模拟支付确认".equals(event.get("remark")))event.put("remark","预订确认完成");
  o.put("refund",db.queryForList("SELECT process,canceltime,review_reason,review_time,refund_amount FROM tdjp WHERE ddid=?",order).stream().map(FlightCatalogService::camel).findFirst().orElse(null));
  if(o.get("priceSnapshot")!=null){JsonNode snapshot=CustomerPresentation.snapshot(decode(o.get("priceSnapshot")));((com.fasterxml.jackson.databind.node.ObjectNode)snapshot).remove("passengers");o.put("priceSnapshot",snapshot);}
  List<Map<String,Object>>passengers=db.queryForList("SELECT passenger_name,seat_no FROM order_passenger WHERE order_id=? ORDER BY passenger_index",order);
  for(Map<String,Object>p:passengers){String name=p.get("passenger_name").toString();p.put("passenger_name",name.substring(0,1)+"**");}o.put("passengers",passengers.stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));return o;
 }
 public Map<String,Object> flightForSnapshot(int flight){
  return FlightCatalogService.camel(catalog.required("SELECT h.flight_no,h.start,h.`end`,h.starttime,h.arrival_time,h.departure_airport,h.arrival_airport,h.departure_terminal,h.arrival_terminal,h.operational_status,h.place,h.duration_minutes,co.company_name,a.name departure_airport_name,b.name arrival_airport_name FROM hbxx h LEFT JOIN company co ON co.company_id=h.company_id LEFT JOIN airport a ON a.code=h.departure_airport LEFT JOIN airport b ON b.code=h.arrival_airport WHERE h.hbid=?",flight));
 }
}
