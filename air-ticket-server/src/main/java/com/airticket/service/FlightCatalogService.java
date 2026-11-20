package com.airticket.service;

import com.airticket.config.ApiException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.*;

@Service
public class FlightCatalogService {
 @Autowired private JdbcTemplate db;
 @Autowired private TransactionTemplate tx;
 @Autowired private DataSource dataSource;
 @Value("${flights.window-days:30}") private int windowDays;
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public LocalDate today(){return LocalDate.now(ZONE);}
 public int windowDays(){return Math.max(1,Math.min(90,windowDays));}
 public static Map<String,Object> camel(Map<String,Object> source){
  Map<String,Object> out=new LinkedHashMap<>();source.forEach((key,value)->{
   StringBuilder name=new StringBuilder();boolean upper=false;for(char c:key.toCharArray()){if(c=='_'){upper=true;continue;}name.append(upper?Character.toUpperCase(c):c);upper=false;}
   out.put(name.toString(),value instanceof LocalDateTime?((LocalDateTime)value).atZone(ZONE).toOffsetDateTime():value);
  });return out;
 }
 public static long epoch(Object value){if(value instanceof LocalDateTime)return ((LocalDateTime)value).atZone(ZONE).toInstant().toEpochMilli();if(value instanceof java.util.Date)return ((java.util.Date)value).getTime();throw new IllegalArgumentException("无效的数据库时间");}
 public Map<String,Object> required(String sql,Object...args){List<Map<String,Object>> rows=db.queryForList(sql,args);if(rows.isEmpty())throw new ApiException(404,"记录不存在");return rows.get(0);}
 public long insert(String sql,Object...args){KeyHolder key=new GeneratedKeyHolder();db.update(c->{PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},key);return Objects.requireNonNull(key.getKey()).longValue();}

 public Map<String,Object> generate(LocalDate start,int days){
  if(days<1||days>90)throw new IllegalArgumentException("生成天数应为1～90");
  try(Connection lock=dataSource.getConnection();PreparedStatement acquire=lock.prepareStatement("SELECT GET_LOCK('air_ticket_flight_generation',0)")){
   try(ResultSet result=acquire.executeQuery()){if(!result.next()||result.getInt(1)!=1)throw new ApiException(409,"航班补齐任务正在运行");}
   long run=insert("INSERT INTO flight_generation_run(window_start,window_end,status)VALUES(?,?,'RUNNING')",start,start.plusDays(days-1));
   int created=0,skipped=0,failed=0;
   try{
    List<Map<String,Object>> templates=db.queryForList("SELECT s.*,a.city departure_city,a.name departure_name,a.terminal departure_terminal,b.city arrival_city,b.terminal arrival_terminal FROM flight_schedule s JOIN airport a ON a.code=s.departure_airport JOIN airport b ON b.code=s.arrival_airport WHERE s.enabled=1 ORDER BY s.id");
    for(LocalDate date=start;date.isBefore(start.plusDays(days));date=date.plusDays(1)){
     for(Map<String,Object> s:templates){
      if(!lock.isValid(2))throw new IllegalStateException("Generation lock connection lost");
      if(date.isBefore(((java.sql.Date)s.get("valid_from")).toLocalDate())||date.isAfter(((java.sql.Date)s.get("valid_to")).toLocalDate())||!s.get("weekdays").toString().contains(Integer.toString(date.getDayOfWeek().getValue())))continue;
      if(db.queryForObject("SELECT COUNT(*) FROM flight_schedule_exception WHERE schedule_id=? AND service_date=?",Integer.class,s.get("id"),date)>0)continue;
      final LocalDate day=date;
      try{Integer n=tx.execute(status->generateOne(s,day));if(n!=null&&n==1)created++;else skipped++;}
      catch(RuntimeException e){failed++;org.slf4j.LoggerFactory.getLogger(getClass()).warn("Flight generation failed for schedule {} date {} ({})",s.get("id"),date,e.getClass().getSimpleName());}
     }
     db.update("UPDATE flight_generation_run SET created_count=?,skipped_count=?,failed_count=? WHERE id=?",created,skipped,failed,run);
    }
    db.update("UPDATE flight_generation_run SET status=?,finished_at=NOW(),error_summary=? WHERE id=?",failed==0?"SUCCESS":"PARTIAL",failed==0?null:"部分实例生成失败，请重试补齐",run);
   }catch(Exception e){db.update("UPDATE flight_generation_run SET status='FAILED',finished_at=NOW(),error_summary='任务中断，可安全重试补齐' WHERE id=?",run);throw e;}
   finally{try(PreparedStatement release=lock.prepareStatement("SELECT RELEASE_LOCK('air_ticket_flight_generation')")){release.execute();}}
   return camel(required("SELECT * FROM flight_generation_run WHERE id=?",run));
  }catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(503,"航班生成失败，请查看任务记录并重试");}
 }
 private int generateOne(Map<String,Object>s,LocalDate day){
  // Lock template before checking: manual edits and generation observe one version.
  required("SELECT id FROM flight_schedule WHERE id=? FOR UPDATE",s.get("id"));
  if(db.queryForObject("SELECT COUNT(*) FROM hbxx WHERE schedule_id=? AND service_date=?",Integer.class,s.get("id"),day)>0)return 0;
  LocalDateTime departure=day.atTime(((Time)s.get("departure_time")).toLocalTime());
  int minutes=((Number)s.get("duration_minutes")).intValue();LocalDateTime arrival=departure.plusMinutes(minutes);
  long id=insert("INSERT INTO hbxx(company_id,xclb,start,`end`,starttime,number,price,duration,place,sale_status,flight_no,schedule_id,schedule_version,service_date,departure_airport,arrival_airport,arrival_time,departure_terminal,arrival_terminal,duration_minutes,layout_code,policy_code,source_type,sale_open_at,sale_close_at)VALUES(?,'国内',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
   s.get("company_id"),s.get("departure_city"),s.get("arrival_city"),departure,0,s.get("base_price"),minutes/60+"小时"+minutes%60+"分钟",s.get("departure_name")+" "+s.get("departure_terminal"),1,s.get("flight_no"),s.get("id"),s.get("version"),day,s.get("departure_airport"),s.get("arrival_airport"),arrival,s.get("departure_terminal"),s.get("arrival_terminal"),minutes,s.get("layout_code"),s.get("policy_code"),"DEMO",day.minusDays(windowDays()-1).atStartOfDay(),departure.minusMinutes(45));
  db.update("INSERT INTO hbxx_cabin(hbid,cabin_class,cabin_name,price,total_number,available_number,status) SELECT ?,cabin_class,CASE cabin_class WHEN 1 THEN '经济舱' ELSE '商务舱' END,ROUND(?*CASE cabin_class WHEN 1 THEN 1 ELSE 1.8 END,2),COUNT(*),COUNT(*),1 FROM layout_seat WHERE layout_code=? GROUP BY cabin_class",id,s.get("base_price"),s.get("layout_code"));
  db.update("INSERT INTO flight_seat(hbid,seat_no,cabin_id,seat_row,seat_column,seat_fee) SELECT ?,ls.seat_no,c.cabin_id,ls.seat_row,ls.seat_column,ls.seat_fee FROM layout_seat ls JOIN hbxx_cabin c ON c.hbid=? AND c.cabin_class=ls.cabin_class WHERE ls.layout_code=?",id,id,s.get("layout_code"));return 1;
 }
 public Map<String,Object> calendar(String start,String end){
  start=normalizeLocation(start);end=normalizeLocation(end);
  Map<String,Object> result=new LinkedHashMap<>();result.put("windowStart",today());result.put("windowEnd",today().plusDays(windowDays()-1));result.put("serverTime",ZonedDateTime.now(ZONE));
  List<Object>args=new ArrayList<>(List.of(today(),today().plusDays(windowDays()-1)));
  String where=" WHERE h.service_date BETWEEN ? AND ?"+visibleFlights();
  where+=locationFilter("departure_airport",start,args);
  where+=locationFilter("arrival_airport",end,args);
  List<Map<String,Object>> rows=db.queryForList("SELECT h.service_date,COUNT(DISTINCT h.hbid) flight_count,MIN(CASE WHEN h.sale_status=1 AND h.operational_status='SCHEDULED' AND NOW() BETWEEN h.sale_open_at AND h.sale_close_at AND c.status=1 AND c.available_number>0 THEN c.price+p.tax_fee+p.fuel_fee END) min_total FROM hbxx h JOIN hbxx_cabin c ON c.hbid=h.hbid JOIN fare_policy p ON p.code=h.policy_code"+where+" GROUP BY h.service_date",args.toArray());
  Map<String,Map<String,Object>> byDate=new HashMap<>();for(Map<String,Object>r:rows)byDate.put(r.get("service_date").toString(),camel(r));
  List<Map<String,Object>> dates=new ArrayList<>();for(int i=0;i<windowDays();i++){String date=today().plusDays(i).toString();Map<String,Object>r=byDate.getOrDefault(date,new LinkedHashMap<>(Map.of("serviceDate",date,"flightCount",0)));r.put("status",r.get("minTotal")!=null?"AVAILABLE":((Number)r.get("flightCount")).intValue()>0?"NOT_AVAILABLE":"NO_DATA");dates.add(r);}result.put("dates",dates);
  result.put("latestRun",db.queryForList("SELECT * FROM flight_generation_run ORDER BY id DESC LIMIT 1").stream().map(FlightCatalogService::camel).findFirst().orElse(null));return result;
 }
 public Map<String,Object> detail(int id){
  Map<String,Object>r=camel(required("SELECT h.*,co.company_name,co.company_picture,a.name departure_airport_name,b.name arrival_airport_name,a.timezone departure_timezone,b.timezone arrival_timezone,l.name layout_name,l.aircraft_type,p.baggage_kg,p.description fare_description,p.tax_fee,p.fuel_fee,p.refund_cutoff_hours FROM hbxx h JOIN company co ON co.company_id=h.company_id LEFT JOIN airport a ON a.code=h.departure_airport LEFT JOIN airport b ON b.code=h.arrival_airport LEFT JOIN aircraft_layout l ON l.code=h.layout_code LEFT JOIN fare_policy p ON p.code=h.policy_code WHERE h.hbid=?",id));
  List<Map<String,Object>> cabins=db.queryForList("SELECT * FROM hbxx_cabin WHERE hbid=? ORDER BY cabin_class",id);r.put("cabins",cabins.stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));
  r.put("number",cabins.stream().filter(c->((Number)c.get("status")).intValue()==1).mapToInt(c->((Number)c.get("available_number")).intValue()).sum());return r;
 }
 public Map<String,Object> search(String start,String end,String date,Integer company,int quantity,String sort,int page,int size){
  start=normalizeLocation(start);end=normalizeLocation(end);
  if(quantity<1||quantity>5||page<1||size<1||size>100)throw new IllegalArgumentException("人数或分页不合法");
  LocalDate day=date==null||date.isBlank()?today():LocalDate.parse(date);
  if(day.isBefore(today())||day.isAfter(today().plusDays(windowDays()-1)))throw new IllegalArgumentException("日期超出售卖窗口");
  List<Object>a=new ArrayList<>(List.of(day));String where=" WHERE h.service_date=?"+visibleFlights();
  where+=locationFilter("departure_airport",start,a);
  where+=locationFilter("arrival_airport",end,a);
  if(company!=null){where+=" AND h.company_id=?";a.add(company);}
  long count=db.queryForObject("SELECT COUNT(*) FROM hbxx h"+where,Long.class,a.toArray());
  List<Object>q=new ArrayList<>();q.add(quantity);q.addAll(a);q.add(size);q.add((page-1)*size);
  String sql="SELECT h.*,co.company_name,co.company_picture,a.name departure_airport_name,b.name arrival_airport_name,l.aircraft_type,p.baggage_kg,p.description fare_description,(SELECT SUM(available_number) FROM hbxx_cabin c WHERE c.hbid=h.hbid AND c.status=1) available_total,(SELECT MIN(c.price+p.tax_fee+p.fuel_fee) FROM hbxx_cabin c WHERE c.hbid=h.hbid AND c.status=1 AND c.available_number>=?) min_total FROM hbxx h JOIN company co ON co.company_id=h.company_id JOIN airport a ON a.code=h.departure_airport JOIN airport b ON b.code=h.arrival_airport JOIN aircraft_layout l ON l.code=h.layout_code JOIN fare_policy p ON p.code=h.policy_code"+where+" ORDER BY "+("price".equals(sort)?"min_total IS NULL,min_total,h.starttime":"h.starttime")+" LIMIT ? OFFSET ?";
  List<Map<String,Object>> records=db.queryForList(sql,q.toArray()).stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList());for(Map<String,Object>r:records)r.put("number",r.remove("availableTotal"));
  return Map.of("total",count,"exists",count>0,"message",count>0?"存在符合条件的航班":"没有符合条件的航班","records",records,"page",page,"pageSize",size);
 }
 // Retired course fixtures remain available to existing orders, but not the reference catalog.
 String visibleFlights(){return " AND NOT EXISTS(SELECT 1 FROM flight_schedule retired WHERE retired.id=h.schedule_id AND retired.enabled=0 AND retired.code LIKE 'COURSE-S%')";}
 String normalizeLocation(String value){
  if(value==null)return null;
  String v=value.trim();if(v.endsWith("市"))v=v.substring(0,v.length()-1);
  Map<String,String> aliases=Map.of("景洪","西双版纳","迪庆","香格里拉","呼伦贝尔","海拉尔","西藏拉萨","拉萨","云南西双版纳","西双版纳","云南香格里拉","香格里拉");
  return aliases.getOrDefault(v,v.length()==3?v.toUpperCase(Locale.ROOT):v);
 }
 String locationFilter(String column,String value,List<Object> args){
  if(value==null||value.isBlank())return "";
  args.add(value);args.add(value);args.add(value);
  return " AND h."+column+" IN(SELECT code FROM airport WHERE code=? OR city=? OR name=?)";
 }
}
