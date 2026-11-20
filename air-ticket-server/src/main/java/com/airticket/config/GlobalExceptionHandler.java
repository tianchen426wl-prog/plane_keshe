package com.airticket.config;
import com.airticket.vo.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class) public ResponseEntity<Result<Void>> business(ApiException e){return ResponseEntity.status(e.getStatus()).body(Result.error(e.getStatus(),e.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.validation.BindException.class,org.springframework.web.bind.MethodArgumentNotValidException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 public ResponseEntity<Result<Void>> invalid(Exception e){return ResponseEntity.badRequest().body(Result.error(400,e instanceof IllegalArgumentException?e.getMessage():"参数错误或缺少必填字段"));}
 @ExceptionHandler({org.springframework.dao.DuplicateKeyException.class,org.springframework.dao.CannotAcquireLockException.class}) public ResponseEntity<Result<Void>> conflict(Exception e){return ResponseEntity.status(409).body(Result.error(409,"数据冲突，请刷新重试"));}
 @ExceptionHandler(Exception.class)public ResponseEntity<Result<Void>> unknown(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed",e);return ResponseEntity.status(500).body(Result.error(500,"服务器处理失败，请稍后重试"));}
}
