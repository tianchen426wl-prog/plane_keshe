package com.airticket.service;

import com.airticket.config.ApiException;
import com.airticket.entity.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** All booking transitions use the same lock order and cabin-only inventory. */
@Service
public class BookingService {
 @Autowired private JdbcTemplate db;
 @Autowired private ObjectMapper json;
 @Autowired private FareSeatService fares;
 @Value("${booking.hold-seconds:900}") private int holdSeconds;
 @Value("${booking.refund-cutoff-hours:2}") private int refundHours;
 private <T>T one(String sql,Class<T> type,Object... args){
  List<T> rows=db.query(sql,new BeanPropertyRowMapper<>(type),args);
  if(rows.isEmpty())throw new ApiException(404,"记录不存在");return rows.get(0);
 }
 private Hbxx flight(int id){return one("SELECT * FROM hbxx WHERE hbid=? FOR UPDATE",Hbxx.class,id);}
 private HbxxCabin cabin(int id){return one("SELECT * FROM hbxx_cabin WHERE cabin_id=? FOR UPDATE",HbxxCabin.class,id);}
 private void sale(Hbxx f){
  fares.sale(db.queryForMap("SELECT * FROM hbxx WHERE hbid=?",f.getHbid()));
  if(!Integer.valueOf(1).equals(f.getSaleStatus())||f.getStarttime()==null||!f.getStarttime().after(new java.util.Date()))throw new ApiException(409,"航班已停售或已起飞");
 }
 private long insert(String sql,Object... args){
  KeyHolder key=new GeneratedKeyHolder();db.update(c->{PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},key);
  return Objects.requireNonNull(key.getKey()).longValue();
 }
 @Transactional
 public Long hold(Integer flightId,Integer cabinId,Integer userId,Integer quantity,String requestId){
  com.airticket.dto.OrderDTO dto=new com.airticket.dto.OrderDTO();dto.setHbid(flightId);dto.setCabinId(cabinId);dto.setQuantity(quantity);dto.setRequestId(requestId);return holdCore(dto,userId);
 }
 @Transactional public Long hold(com.airticket.dto.OrderDTO dto,Integer userId){return holdCore(dto,userId);}
 private Long holdCore(com.airticket.dto.OrderDTO dto,Integer userId){
  Integer flightId=dto.getHbid(),cabinId=dto.getCabinId(),quantity=dto.getQuantity();String requestId=dto.getRequestId();
  String fingerprint=org.springframework.util.DigestUtils.md5DigestAsHex((String.valueOf(flightId)+"|"+cabinId+"|"+quantity+"|"+dto.getQuoteId()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  if(flightId==null||cabinId==null||quantity==null||quantity<1||quantity>5)throw new IllegalArgumentException("请选择航班舱位，购买数量应为1～5张");
  if(requestId==null)requestId=UUID.randomUUID().toString();
  if(!requestId.matches("[A-Za-z0-9_-]{8,64}"))throw new IllegalArgumentException("请求编号不合法");
  User u=one("SELECT * FROM `user` WHERE id=? FOR UPDATE",User.class,userId);
  if(!Integer.valueOf(1).equals(u.getStatus()))throw new ApiException(403,"账号已停用");
  List<SeatHold> previous=db.query("SELECT * FROM seat_hold WHERE userid=? AND request_id=?",new BeanPropertyRowMapper<>(SeatHold.class),userId,requestId);
  if(!previous.isEmpty()){
   SeatHold h=previous.get(0);
   if(!flightId.equals(h.getHbid())||!cabinId.equals(h.getCabinId())||!quantity.equals(h.getQuantity()))throw new ApiException(409,"同一请求不能修改购票内容");
   String old=db.queryForObject("SELECT request_fingerprint FROM seat_hold WHERE hold_id=?",String.class,h.getHoldId());
   if(old!=null&&!old.equals(fingerprint))throw new ApiException(409,"同一请求不能修改报价或座位");
   return h.getHoldId();
  }
  if(db.queryForObject("SELECT COUNT(*) FROM seat_hold WHERE userid=? AND status=0",Integer.class,userId)>=3)throw new ApiException(409,"最多保留3个未完成占位，请先处理已有占位");
  Hbxx f=flight(flightId);sale(f);HbxxCabin c=cabin(cabinId);
  if(!flightId.equals(c.getHbid())||!Integer.valueOf(1).equals(c.getStatus()))throw new ApiException(409,"舱位已停用或不属于此航班");
  if(db.update("UPDATE hbxx_cabin SET available_number=available_number-? WHERE cabin_id=? AND available_number>=?",quantity,cabinId,quantity)!=1)throw new ApiException(409,"该舱位余票不足");
  long id=insert("INSERT INTO seat_hold(hbid,cabin_id,userid,quantity,create_time,expire_time,status,unit_price,request_id)VALUES(?,?,?,?,NOW(),DATE_ADD(NOW(),INTERVAL ? SECOND),0,?,?)",flightId,cabinId,userId,quantity,holdSeconds,c.getPrice(),requestId);
  db.update("UPDATE seat_hold SET request_fingerprint=? WHERE hold_id=?",fingerprint,id);fares.attach(id,dto,userId);return id;
 }
 private SeatHold ownedHold(Long id,Integer user){
  if(id==null)throw new IllegalArgumentException("缺少占位编号");
  SeatHold h=one("SELECT * FROM seat_hold WHERE hold_id=? FOR UPDATE",SeatHold.class,id);
  if(!h.getUserid().equals(user))throw new ApiException(403,"无权操作此占位");return h;
 }
 private void release(SeatHold h,int target){
  flight(h.getHbid());cabin(h.getCabinId());
  if(db.update("UPDATE seat_hold SET status=? WHERE hold_id=? AND status=0",target,h.getHoldId())!=1)throw new ApiException(409,"占位状态已改变");
  fares.release(h.getHoldId());
  if(db.update("UPDATE hbxx_cabin SET available_number=available_number+? WHERE cabin_id=? AND available_number+?<=total_number",h.getQuantity(),h.getCabinId(),h.getQuantity())!=1)throw new ApiException(409,"库存校验失败，请联系管理员");
 }
 @Transactional
 public boolean confirm(Long id,Integer user){
  SeatHold h=ownedHold(id,user);
  if(h.getStatus()==1)return true;
  if(h.getStatus()!=0)throw new ApiException(409,"该占位已终止");
  if(!h.getExpireTime().after(new java.util.Date())){release(h,2);return false;}
  Hbxx f=flight(h.getHbid());sale(f);HbxxCabin c=cabin(h.getCabinId());
  if(!Integer.valueOf(1).equals(c.getStatus()))throw new ApiException(409,"舱位已停用，请取消占位");
  BigDecimal price=h.getUnitPrice();if(price==null)throw new ApiException(409,"历史占位缺少价格，请重新占位");
  Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("flightNo",f.getFlightNo());snapshot.put("start",f.getStart());snapshot.put("end",f.getEnd());snapshot.put("starttime",f.getStarttime());snapshot.put("place",f.getPlace());snapshot.put("cabinName",c.getCabinName());
  Map<String,Object> detail=fares.flightForSnapshot(f.getHbid());
  snapshot.putAll(detail);
  String data;try{data=json.writeValueAsString(snapshot);}catch(JsonProcessingException e){throw new IllegalStateException(e);}
  BigDecimal total=db.queryForObject("SELECT COALESCE(total_cost,unit_price*quantity) FROM seat_hold WHERE hold_id=?",BigDecimal.class,id);
  long order=insert("INSERT INTO jpdd(hbid,userid,cabin_id,quantity,cost,unit_price,ordertime,status,hold_id,order_no,flight_snapshot)VALUES(?,?,?,?,?,?,NOW(),1,?,?,?)",h.getHbid(),user,h.getCabinId(),h.getQuantity(),total,price,id,"AT"+UUID.randomUUID().toString().replace("-",""),data);
  fares.confirm(id,order);
  db.update("UPDATE seat_hold SET status=1 WHERE hold_id=? AND status=0",id);log(order,"PAY",user,"预订确认完成");return true;
 }
 @Transactional public void cancel(Long id,Integer user){SeatHold h=ownedHold(id,user);if(h.getStatus()==3||h.getStatus()==2)return;if(h.getStatus()!=0)throw new ApiException(409,"已购票的占位不能取消");release(h,3);}
 @Transactional public void expire(Long id){
  SeatHold h=one("SELECT * FROM seat_hold WHERE hold_id=? FOR UPDATE",SeatHold.class,id);
  if(h.getStatus()==0&&!h.getExpireTime().after(new java.util.Date()))release(h,2);
 }
 @Transactional public void requestRefund(Integer id,Integer user){
  Jpdd order=one("SELECT * FROM jpdd WHERE ddid=? FOR UPDATE",Jpdd.class,id);
  if(!order.getUserid().equals(user))throw new ApiException(403,"无权操作此订单");
  if(order.getStatus()==2)return;
  if(order.getStatus()!=1)throw new ApiException(409,"订单已退订");
  Hbxx f=flight(order.getHbid());
  boolean itemized=db.queryForObject("SELECT price_snapshot IS NOT NULL FROM jpdd WHERE ddid=?",Boolean.class,id);
  Map<String,Object> refundQuote=itemized?fares.refundQuote(id,user):null;
  if(!itemized&&f.getStarttime().getTime()-System.currentTimeMillis()<refundHours*3600000L)throw new ApiException(409,"已超过退订截止时间（起飞前"+refundHours+"小时）");
  db.update("UPDATE jpdd SET status=2 WHERE ddid=?",id);
  db.update("INSERT INTO tdjp(ddid,canceltime,process)VALUES(?,NOW(),0) ON DUPLICATE KEY UPDATE canceltime=NOW(),process=0,review_reason=NULL,reviewer_id=NULL,review_time=NULL",id);
  if(itemized)db.update("UPDATE tdjp SET refund_amount=? WHERE ddid=?",refundQuote.get("amount"),id);
  log(id,"REFUND_REQUEST",user,"用户申请退订");
 }
 @Transactional public void review(Integer refundId,boolean approved,String reason,Integer admin){
  // Read immutable order ID first; then every refund path locks order before refund.
  Tdjp initial=one("SELECT * FROM tdjp WHERE id=?",Tdjp.class,refundId);
  Jpdd order=one("SELECT * FROM jpdd WHERE ddid=? FOR UPDATE",Jpdd.class,initial.getDdid());
  Tdjp refund=one("SELECT * FROM tdjp WHERE id=? FOR UPDATE",Tdjp.class,refundId);
  int target=approved?1:2;
  if(refund.getProcess()==target)return;
  if(refund.getProcess()!=0||order.getStatus()!=2)throw new ApiException(409,"该申请已处理");
  if(reason==null||reason.isBlank()||reason.length()>500)throw new IllegalArgumentException("审核意见应为1～500字");
  if(approved){flight(order.getHbid());cabin(order.getCabinId());
   if(order.getHoldId()!=null)fares.release(order.getHoldId());
   db.update("UPDATE jpdd SET refund_amount=(SELECT COALESCE(refund_amount,?) FROM tdjp WHERE id=?) WHERE ddid=?",order.getCost(),refundId,order.getDdid());
   if(db.update("UPDATE hbxx_cabin SET available_number=available_number+? WHERE cabin_id=? AND available_number+?<=total_number",order.getQuantity(),order.getCabinId(),order.getQuantity())!=1)throw new ApiException(409,"退款库存校验失败");
  }
  db.update("UPDATE jpdd SET status=? WHERE ddid=?",approved?3:1,order.getDdid());
  db.update("UPDATE tdjp SET process=?,review_reason=?,reviewer_id=?,review_time=NOW() WHERE id=?",target,reason,admin,refundId);
  log(order.getDdid(),approved?"REFUND_APPROVE":"REFUND_REJECT",admin,reason);
 }
 private void log(long id,String action,Integer actor,String reason){db.update("INSERT INTO order_log(ddid,action,operator,remark)VALUES(?,?,?,?)",id,action,actor.toString(),reason);}
}
