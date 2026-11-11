package com.minimall.common.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class Digests {
  private Digests() {}

  public static String sha256(String input) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean equal(String first, String second) {
    return first != null
        && second != null
        && MessageDigest.isEqual(
            first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
  }
}
