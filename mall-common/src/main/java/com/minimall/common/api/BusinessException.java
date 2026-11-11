package com.minimall.common.api;

public class BusinessException extends RuntimeException {
  private final String code;
  private final int httpStatus;

  public BusinessException(String code, int httpStatus, String message) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public String code() {
    return code;
  }

  public int httpStatus() {
    return httpStatus;
  }

  public static BusinessException bad(String message) {
    return new BusinessException("INVALID_ARGUMENT", 400, message);
  }

  public static BusinessException conflict(String message) {
    return new BusinessException("STATE_CONFLICT", 409, message);
  }

  public static BusinessException missing() {
    return new BusinessException("NOT_FOUND", 404, "资源不存在");
  }

  public static BusinessException unauthorized() {
    return new BusinessException("UNAUTHENTICATED", 401, "请重新登录");
  }

  public static BusinessException unavailable() {
    return new BusinessException("DEPENDENCY_UNAVAILABLE", 503, "依赖服务暂时不可用");
  }
}
