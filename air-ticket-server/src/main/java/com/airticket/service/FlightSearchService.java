package com.airticket.service;

import com.airticket.dto.FlightFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightSearchService {
 @Autowired private JdbcTemplate db;
 @Autowired private FlightCatalogService catalog;
 private String query(String start,String end,LocalDate from,LocalDate to,FlightFilter f,List<Object> args){
  f.validate();args.add(f.getQuantity());
  String cabin=" WHERE status=1";
  if(f.getCabinClass()!=null){cabin+=" AND cabin_class=?";args.add(f.getCabinClass());}
  String sql=" FROM hbxx h JOIN company co ON co.company_id=h.company_id JOIN airport a ON a.code=h.departure_airport JOIN airport b ON b.code=h.arrival_airport JOIN aircraft_layout l ON l.code=h.layout_code JOIN fare_policy p ON p.code=h.policy_code LEFT JOIN (SELECT hbid,MIN(CASE WHEN available_number>=? THEN price END) base_min,SUM(available_number) available_total FROM hbxx_cabin"+cabin+" GROUP BY hbid) c ON c.hbid=h.hbid WHERE h.service_date BETWEEN ? AND ?";
  args.add(from);args.add(to);
  sql+=catalog.visibleFlights();
  sql+=catalog.locationFilter("departure_airport",catalog.normalizeLocation(start),args);
  sql+=catalog.locationFilter("arrival_airport",catalog.normalizeLocation(end),args);
  if(f.getCabinClass()!=null&&!f.isOnlyAvailable()){sql+=" AND EXISTS(SELECT 1 FROM hbxx_cabin requested WHERE requested.hbid=h.hbid AND requested.status=1 AND requested.cabin_class=?)";args.add(f.getCabinClass());}
  if(f.getCompanyId()!=null){sql+=" AND h.company_id=?";args.add(f.getCompanyId());}
  if(f.getDepartureFrom()!=null){sql+=" AND HOUR(h.starttime)>=?";args.add(f.getDepartureFrom());}
  if(f.getDepartureTo()!=null){sql+=" AND HOUR(h.starttime)<?";args.add(f.getDepartureTo());}
  if(f.getMinBaggage()!=null){sql+=" AND p.baggage_kg>=?";args.add(f.getMinBaggage());}
  if(f.getMaxTotal()!=null&&f.getMaxTotal().signum()>0){sql+=" AND (c.base_min+p.tax_fee+p.fuel_fee)*?<=?";args.add(f.getQuantity());args.add(f.getMaxTotal());}
  if(f.isOnlyAvailable())sql+=" AND c.base_min IS NOT NULL AND "+sale();
  return sql;
 }
 private String sale(){return "h.sale_status=1 AND h.operational_status='SCHEDULED' AND (h.sale_open_at IS NULL OR NOW()>=h.sale_open_at) AND (h.sale_close_at IS NULL OR NOW()<h.sale_close_at) AND h.starttime>NOW()";}
 private String bookingStatus(){return "CASE WHEN h.operational_status='CANCELLED' THEN 'CANCELLED' WHEN h.starttime<=NOW() THEN 'DEPARTED' WHEN h.sale_status<>1 THEN 'NOT_ON_SALE' WHEN h.operational_status<>'SCHEDULED' THEN 'FLIGHT_CHANGED' WHEN NOW()<h.sale_open_at THEN 'NOT_OPEN' WHEN NOW()>=h.sale_close_at THEN 'SALES_CLOSED' WHEN c.available_total IS NULL THEN 'INVENTORY_UNAVAILABLE' WHEN c.available_total=0 THEN 'SOLD_OUT' WHEN c.base_min IS NULL THEN 'INSUFFICIENT_SEATS' ELSE 'AVAILABLE' END";}
 public Map<String,Object> search(String start,String end,String date,FlightFilter filter,String sort,int page,int size,int days){
  if(page<1||size<1||size>100||!Set.of("price","time","arrival","duration").contains(sort))throw new IllegalArgumentException("分页或排序条件不合法");
  LocalDate today=LocalDate.now(FlightCatalogService.ZONE),day=date==null||date.isBlank()?today:LocalDate.parse(date);
  if(day.isBefore(today)||day.isAfter(today.plusDays(days-1)))throw new IllegalArgumentException("日期超出售卖窗口");
  List<Object> args=new ArrayList<>();String from=query(start,end,day,day,filter,args);
  long total=db.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());
  String order=Map.of("price","min_total IS NULL,min_total,h.starttime,h.hbid","time","h.starttime,h.hbid","arrival","h.arrival_time,h.hbid","duration","h.duration_minutes,h.starttime,h.hbid").get(sort);
  args.add(size);args.add((long)(page-1)*size);
  String sql="SELECT h.*,co.company_name,co.company_picture,a.name departure_airport_name,b.name arrival_airport_name,l.aircraft_type,p.baggage_kg,p.description fare_description,c.available_total,(c.base_min+p.tax_fee+p.fuel_fee) min_total,"+bookingStatus()+" booking_status"+from+" ORDER BY "+order+" LIMIT ? OFFSET ?";
  List<Map<String,Object>> records=db.queryForList(sql,args.toArray()).stream().map(FlightCatalogService::camel).collect(Collectors.toList());
  // Do not alias over h.number: ColumnMapRowMapper preserves the first duplicate label.
  for(Map<String,Object> record:records)record.put("number",record.remove("availableTotal"));
  return Map.of("total",total,"exists",total>0,"message",total>0?"存在符合条件的航班":"没有符合条件的航班","records",records,"page",page,"pageSize",size,"quantity",filter.getQuantity(),"serverTime",ZonedDateTime.now(FlightCatalogService.ZONE));
 }
 public Map<String,Object> calendar(String start,String end,FlightFilter filter,int days){
  LocalDate today=LocalDate.now(FlightCatalogService.ZONE);List<Object> args=new ArrayList<>();String from=query(start,end,today,today.plusDays(days-1),filter,args);
  List<Map<String,Object>> rows=db.queryForList("SELECT h.service_date,COUNT(*) flight_count,MIN(CASE WHEN "+sale()+" THEN (c.base_min+p.tax_fee+p.fuel_fee)*"+filter.getQuantity()+" END) min_total"+from+" GROUP BY h.service_date",args.toArray());
  Map<String,Map<String,Object>> index=new HashMap<>();for(Map<String,Object> row:rows)index.put(row.get("service_date").toString(),FlightCatalogService.camel(row));
  List<Map<String,Object>> dates=new ArrayList<>();for(int i=0;i<days;i++){String date=today.plusDays(i).toString();Map<String,Object> row=index.getOrDefault(date,new LinkedHashMap<>(Map.of("serviceDate",date,"flightCount",0)));row.put("status",row.get("minTotal")!=null?"AVAILABLE":((Number)row.get("flightCount")).intValue()>0?"NOT_AVAILABLE":"NO_DATA");dates.add(row);}
  Map<String,Object> result=new LinkedHashMap<>();result.put("dates",dates);result.put("windowStart",today);result.put("windowEnd",today.plusDays(days-1));result.put("quantity",filter.getQuantity());result.put("serverTime",ZonedDateTime.now(FlightCatalogService.ZONE));result.put("latestRun",db.queryForList("SELECT * FROM flight_generation_run ORDER BY id DESC LIMIT 1").stream().map(FlightCatalogService::camel).findFirst().orElse(null));return result;
 }
}
