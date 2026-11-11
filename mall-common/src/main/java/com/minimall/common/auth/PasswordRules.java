package com.minimall.common.auth;

import com.minimall.common.api.BusinessException;
import java.nio.charset.StandardCharsets;

public final class PasswordRules {
  private PasswordRules() {}

  public static void validate(String value) {
    if (value == null
        || value.length() < 8
        || value.length() > 64
        || value.getBytes(StandardCharsets.UTF_8).length > 72)
      throw BusinessException.bad("密码长度为8至64字符且不超过72字节");
  }

  public static String login(String value) {
    if (value == null) throw BusinessException.bad("用户名不能为空");
    String normalized = value.strip().toLowerCase(java.util.Locale.ROOT);
    if (!normalized.matches("[a-z0-9_]{4,32}")) throw BusinessException.bad("用户名为4至32位字母数字下划线");
    return normalized;
  }
}
