package com.minimall.common.auth;
import java.time.Instant;
public record SessionView(String token,Instant expiresAt) {}
