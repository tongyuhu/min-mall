package com.minimall.app.security;

import com.minimall.common.api.ApiError;
import com.minimall.common.util.Json;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;

final class SecurityErrors {
  private SecurityErrors() {}

  static void write(
      HttpServletResponse response, Json json, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response
        .getWriter()
        .write(json.write(new ApiError(code, message, MDC.get("traceId"), Map.of())));
  }
}
