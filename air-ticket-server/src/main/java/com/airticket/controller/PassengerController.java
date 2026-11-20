package com.airticket.controller;

import com.airticket.config.ApiException;
import com.airticket.util.CurrentUser;
import com.airticket.vo.Result;
import com.airticket.service.FlightCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/passengers")
public class PassengerController {
 @Autowired private JdbcTemplate db;
 private String name(Map<String,String> body){
  String name=body.getOrDefault("name","");
  if(name==null||name.trim().isEmpty()||name.trim().length()>50||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("乘机人姓名应为1～50字，不能包含控制字符");
  return name.trim();
 }
 @GetMapping public Result<?> list(){return Result.success(db.queryForList("SELECT id,name FROM passenger_profile WHERE userid=? ORDER BY id",CurrentUser.id()).stream().map(FlightCatalogService::camel).collect(Collectors.toList()));}
 @PostMapping @Transactional public Result<?> create(@RequestBody Map<String,String> body){
  int user=CurrentUser.id();String name=name(body);
  db.queryForList("SELECT id FROM `user` WHERE id=? FOR UPDATE",user);
  if(db.queryForObject("SELECT COUNT(*) FROM passenger_profile WHERE userid=?",Integer.class,user)>=20)throw new ApiException(409,"最多保存20位常用乘机人");
  if(db.queryForObject("SELECT COUNT(*) FROM passenger_profile WHERE userid=? AND name=?",Integer.class,user,name)>0)throw new ApiException(409,"该乘机人已保存");
  db.update("INSERT INTO passenger_profile(userid,name)VALUES(?,?)",user,name);return list();
 }
 @PutMapping("/{id}") @Transactional public Result<?> update(@PathVariable long id,@RequestBody Map<String,String> body){
  int user=CurrentUser.id();String name=name(body);db.queryForList("SELECT id FROM `user` WHERE id=? FOR UPDATE",user);
  if(db.queryForObject("SELECT COUNT(*) FROM passenger_profile WHERE userid=? AND name=? AND id<>?",Integer.class,user,name,id)>0)throw new ApiException(409,"该乘机人已保存");
  if(db.update("UPDATE passenger_profile SET name=? WHERE id=? AND userid=?",name,id,user)!=1)throw new ApiException(404,"乘机人不存在");return list();
 }
 @DeleteMapping("/{id}") public Result<?> delete(@PathVariable long id){if(db.update("DELETE FROM passenger_profile WHERE id=? AND userid=?",id,CurrentUser.id())!=1)throw new ApiException(404,"乘机人不存在");return Result.success();}
}
