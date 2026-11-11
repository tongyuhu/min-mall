package com.minimall.common.auth;

import java.util.Optional;

public interface AccountDirectory {
  ActorType type();

  Optional<AccountIdentity> findByLogin(String login);

  Optional<AccountIdentity> findById(long id);
}
