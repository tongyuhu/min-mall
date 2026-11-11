package com.minimall.common.auth;

import java.util.Set;

public record AccountIdentity(
    ActorType type,
    long id,
    String passwordHash,
    boolean enabled,
    long authVersion,
    Set<String> permissions) {
  public AccountIdentity {
    permissions = Set.copyOf(permissions);
  }

  public AccountIdentity withoutCredentials() {
    return new AccountIdentity(type, id, null, enabled, authVersion, permissions);
  }
}
