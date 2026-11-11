package com.minimall.common.util;import java.util.UUID;
public final class BusinessNumbers {private BusinessNumbers(){}public static String next(String prefix){return prefix+UUID.randomUUID().toString().replace("-","");}}
