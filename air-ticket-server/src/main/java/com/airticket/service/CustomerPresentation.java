package com.airticket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Adapt legacy display text without changing stored transaction snapshots. */
public final class CustomerPresentation {
 private CustomerPresentation(){}
 public static Object remark(Object value){return "模拟支付确认".equals(value)?"预订确认完成":value;}
 private static boolean obsolete(String text){return text.matches("(?s).*(课程|演示|教学|参考|非实时).*");}
 static JsonNode snapshot(JsonNode original){
  JsonNode copy=original.deepCopy();if(!copy.isObject())return copy;
  ObjectNode root=(ObjectNode)copy;
  if(root.path("flightNo").asText().matches("参考[0-9]+"))root.put("flightNo","航班"+root.path("flightNo").asText().substring(2));
  if(obsolete(root.path("notice").asText()))root.put("notice","费用以订单明细为准。");
  for(JsonNode item:root.path("items"))if(item.isObject()&&obsolete(item.path("name").asText())){
   String code=item.path("code").asText();((ObjectNode)item).put("name","TAX".equals(code)?"税费":"FUEL".equals(code)?"燃油附加费":"服务费用");
  }
  JsonNode policy=root.path("policy");if(policy.isObject()){
   if(obsolete(policy.path("name").asText()))((ObjectNode)policy).put("name","国内成人票");
   if(obsolete(policy.path("description").asText()))((ObjectNode)policy).put("description","托运行李"+policy.path("baggageKg").asInt()+"kg/人。退订按购票时规则办理。");
  }
  return root;
 }
}
