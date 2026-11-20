package com.airticket.service;
import com.airticket.config.ApiException;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class DeepSeekProvider implements LlmProvider {
 @Value("${ai.enabled:false}") private boolean enabled;
 @Value("${ai.deepseek.api-key:}") private String apiKey;
 @Value("${ai.deepseek.model:deepseek-flash}") private String model;
 @Value("${ai.deepseek.base-url:https://api.deepseek.com}") private String baseUrl;
 @Autowired private ObjectMapper mapper;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public boolean available(){return enabled&&!apiKey.isBlank();}
 public JsonNode json(String system,String input,long deadline){
  if(!available())throw new ApiException(503,"AI_NOT_CONFIGURED：智能规划未启用，请使用普通查询");
  long remaining=(deadline-System.nanoTime())/1000000;if(remaining<100)throw new ApiException(504,"智能规划超时，请使用普通查询");
  try{
   // One fixed provider host. Neither a user nor model can select an outbound URL.
   URI uri=URI.create(baseUrl+"/chat/completions");
   if(!"api.deepseek.com".equals(uri.getHost())||!"https".equals(uri.getScheme()))throw new IllegalStateException("Unsupported provider endpoint");
   Map<String,Object>body=new LinkedHashMap<>();body.put("model",model);body.put("messages",List.of(Map.of("role","system","content",system+" Return only valid json."),Map.of("role","user","content",input)));body.put("response_format",Map.of("type","json_object"));body.put("max_tokens",900);body.put("thinking",Map.of("type","disabled"));body.put("stream",false);
   HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(remaining)).header("Content-Type","application/json").header("Authorization","Bearer "+apiKey).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
   HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw new ApiException(503,"DeepSeek 服务暂不可用（HTTP "+response.statusCode()+"），请使用普通查询");
   if(response.body().length()>100000)throw new ApiException(502,"模型响应过大");
   JsonNode envelope=mapper.readTree(response.body());String content=envelope.path("choices").path(0).path("message").path("content").asText();
   if(content.isBlank())throw new ApiException(502,"模型未返回有效规划内容");return mapper.readTree(content);
  }catch(ApiException e){throw e;}catch(java.net.http.HttpTimeoutException e){throw new ApiException(504,"DeepSeek 请求超时，请使用普通查询");}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ApiException(502,"DeepSeek 连接或响应异常，请使用普通查询");}
 }
}
