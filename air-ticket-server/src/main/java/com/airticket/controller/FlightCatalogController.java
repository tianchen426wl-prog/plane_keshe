package com.airticket.controller;
import com.airticket.service.*;
import com.airticket.dto.OrderDTO;
import com.airticket.util.CurrentUser;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class FlightCatalogController {
 @Autowired private FlightCatalogService catalog;
 @Autowired private FareSeatService fares;
 @Autowired private FlightSearchService searchService;
 @Autowired private JdbcTemplate db;
 @GetMapping("/api/airports") public Result<?> airports(){return Result.success(db.queryForList("SELECT * FROM airport ORDER BY city,code").stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));}
 @GetMapping("/api/flights/calendar") public Result<?> calendar(@RequestParam(required=false)String start,@RequestParam(required=false)String end,@ModelAttribute com.airticket.dto.FlightFilter filter){return Result.success(searchService.calendar(start,end,filter,catalog.windowDays()));}
 @GetMapping("/api/flights") public Result<?> search(@RequestParam(required=false)String start,@RequestParam(required=false)String end,@RequestParam(required=false)String date,@ModelAttribute com.airticket.dto.FlightFilter filter,@RequestParam(defaultValue="time")String sort,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return Result.success(searchService.search(start,end,date,filter,sort,page,pageSize,catalog.windowDays()));}
 @GetMapping("/api/flights/{id}") public Result<?> detail(@PathVariable int id){return Result.success(catalog.detail(id));}
 @GetMapping("/api/flights/{id}/seats") public Result<?> seats(@PathVariable int id,@RequestParam int cabinId){return Result.success(fares.seats(id,cabinId));}
 @PostMapping("/api/quotes") public Result<?> quote(@RequestBody OrderDTO dto){return Result.success(fares.quote(dto,CurrentUser.id()));}
 @GetMapping("/api/jpdd/holds/{id}") public Result<?> hold(@PathVariable long id){return Result.success(fares.holdDetail(id,CurrentUser.id()));}
 @GetMapping("/api/jpdd/orders/{id}/details") public Result<?> order(@PathVariable int id){return Result.success(fares.orderDetail(id,CurrentUser.id()));}
 @GetMapping("/api/jpdd/orders/{id}/refund-quote") public Result<?> refund(@PathVariable int id){return Result.success(fares.refundQuote(id,CurrentUser.id()));}
 @GetMapping("/api/admin/hbxx/schedules") public Result<?> schedules(){return Result.success(db.queryForList("SELECT s.*,c.company_name,a.city departure_city,b.city arrival_city FROM flight_schedule s JOIN company c ON c.company_id=s.company_id JOIN airport a ON a.code=s.departure_airport JOIN airport b ON b.code=s.arrival_airport ORDER BY s.id").stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));}
 @PutMapping("/api/admin/hbxx/schedules/{id}/enabled") public Result<?> enabled(@PathVariable long id,@RequestBody Map<String,Boolean> body){if(body.get("enabled")==null)throw new IllegalArgumentException("缺少启停状态");if(db.update("UPDATE flight_schedule SET enabled=?,version=version+1 WHERE id=?",body.get("enabled"),id)!=1)throw new com.airticket.config.ApiException(404,"班期不存在");return Result.success();}
 @GetMapping("/api/admin/hbxx/generation-runs") public Result<?> runs(){return Result.success(db.queryForList("SELECT * FROM flight_generation_run ORDER BY id DESC LIMIT 30").stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));}
 @PostMapping("/api/admin/hbxx/generate") public Result<?> generate(){return Result.success(catalog.generate(catalog.today(),catalog.windowDays()));}
 @GetMapping("/api/admin/hbxx/fare-policies") public Result<?> policies(){return Result.success(db.queryForList("SELECT * FROM fare_policy ORDER BY code").stream().map(FlightCatalogService::camel).collect(java.util.stream.Collectors.toList()));}
}
