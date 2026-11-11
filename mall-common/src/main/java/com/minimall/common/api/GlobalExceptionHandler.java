package com.minimall.common.api;

import java.util.Map;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> business(BusinessException e) {
    return error(e.httpStatus(), e.code(), e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.bind.ServletRequestBindingException.class,
    IllegalArgumentException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> invalid(Exception e) {
    return error(400, "INVALID_ARGUMENT", "请求参数不合法");
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<ApiError> missingRoute(Exception e) {
    return error(404, "NOT_FOUND", "接口或资源不存在");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> unsupportedMethod(Exception e) {
    return error(405, "METHOD_NOT_ALLOWED", "请求方法不支持");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> denied(Exception e) {
    return error(403, "FORBIDDEN", "没有操作权限");
  }

  @ExceptionHandler({
    DuplicateKeyException.class,
    DataIntegrityViolationException.class,
    CannotAcquireLockException.class
  })
  public ResponseEntity<ApiError> conflict(Exception e) {
    return error(409, "DATA_CONFLICT", "数据冲突，请刷新后重试");
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiError> database(Exception e) {
    LOG.error("数据库操作失败，类型={}", e.getClass().getSimpleName());
    return error(503, "DEPENDENCY_UNAVAILABLE", "依赖服务暂时不可用");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unknown(Exception e) {
    LOG.error("未处理异常，类型={}", e.getClass().getName());
    return error(500, "INTERNAL_ERROR", "服务内部错误");
  }

  private ResponseEntity<ApiError> error(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ApiError(code, message, MDC.get("traceId"), Map.of()));
  }
}
