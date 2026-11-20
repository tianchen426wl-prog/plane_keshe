package com.airticket.config;

import com.airticket.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@Component
public class SecurityFilter extends OncePerRequestFilter {
 @Autowired private JwtUtil jwt;
 @Autowired private JdbcTemplate db;
 @Autowired private ObjectMapper json;
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  try { authorize(req); }
  catch(ApiException e) {res.setStatus(e.getStatus());res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),com.airticket.vo.Result.error(e.getStatus(),e.getMessage()));return;}
  chain.doFilter(req,res);
 }
 private void authorize(HttpServletRequest r) {
  String path=r.getRequestURI(); if(!path.startsWith("/api/"))return;
  for(String key:new String[]{"page","pageSize"}) if(r.getParameter(key)!=null) {
   try {int n=Integer.parseInt(r.getParameter(key));if(n<1 || (key.equals("pageSize")&&n>100))throw new NumberFormatException();}
   catch(NumberFormatException e){throw new ApiException(400,"分页参数不合法");}
  }
  if("OPTIONS".equals(r.getMethod()))return;
  if(Set.of("/api/admin/login","/api/user/login","/api/user/register").contains(path))return;
  boolean admin=path.startsWith("/api/admin/");
  boolean personal=path.startsWith("/api/user/")||path.startsWith("/api/jpdd/")||path.equals("/api/quotes")||path.startsWith("/api/ai/");
  boolean read=path.startsWith("/api/company/")||path.startsWith("/api/hbxx/")||path.startsWith("/api/news/");
  if(read && "GET".equals(r.getMethod()))return;
  if(read)throw new ApiException(403,"请使用后台管理接口");
  if("GET".equals(r.getMethod())&&(path.equals("/api/airports")||path.equals("/api/flights")||path.startsWith("/api/flights/")||path.equals("/api/ai/capabilities")))return;
  if(!admin&&!personal)throw new ApiException(404,"接口不存在");
  String h=r.getHeader("Authorization");
  Claims c=h!=null&&h.startsWith("Bearer ")?jwt.parseToken(h.substring(7)):null;
  if(c==null || !(admin?"ADMIN":"USER").equals(c.get("kind")) || !(c.get("id") instanceof Number) || !(c.get("version") instanceof Number))throw new ApiException(401,"请重新登录");
  int id=((Number)c.get("id")).intValue();
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM "+(admin?"admin":"`user`")+" WHERE id=?",id);
  if(rows.isEmpty())throw new ApiException(401,"账号不存在");
  Map<String,Object>a=rows.get(0);
  if(((Number)a.get("status")).intValue()!=1 || ((Number)a.get("auth_version")).intValue()!=((Number)c.get("version")).intValue())throw new ApiException(401,"账号或权限已改变，请重新登录");
  r.setAttribute("principalId",id);
  if(!admin)return;
  int role=((Number)a.get("role")).intValue();r.setAttribute("adminId",id);r.setAttribute("role",role);
  if(path.equals("/api/admin/info")||path.equals("/api/admin/logout")||role==0)return;
  if(path.startsWith("/api/admin/menu/")||path.startsWith("/api/admin/role/"))throw new ApiException(403,"仅超级管理员可操作");
  String module=path.substring(11).split("/")[0];
  String menu=Map.of("user","user","company","company","hbxx","flight","jpdd","order","tdjp","refund","news","news").get(module);
  if(menu==null || db.queryForObject("SELECT COUNT(*) FROM admin_menu am JOIN menu m ON m.id=am.menu_id WHERE am.admin_id=? AND m.path=?",Integer.class,id,"/admin/"+menu)==0)throw new ApiException(403,"没有此功能权限");
 }
}
