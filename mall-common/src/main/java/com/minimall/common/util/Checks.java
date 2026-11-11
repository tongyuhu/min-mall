package com.minimall.common.util;

import com.minimall.common.api.BusinessException;

public final class Checks {
  private Checks() {}

  public static String text(String value, int min, int max, String name) {
    if (value == null || value.strip().length() < min || value.strip().length() > max)
      throw BusinessException.bad(name + "长度不合法");
    return value.strip();
  }

  public static int quantity(int value) {
    if (value < 1 || value > 99) throw BusinessException.bad("数量必须为1至99");
    return value;
  }

  public static long id(String value) {
    try {
      long id = Long.parseLong(value);
      if (id <= 0) throw new NumberFormatException();
      return id;
    } catch (RuntimeException e) {
      throw BusinessException.bad("ID不合法");
    }
  }

  public static void state(boolean condition, String message) {
    if (!condition) throw BusinessException.conflict(message);
  }
}
