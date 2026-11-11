package com.minimall.user.account.application;

import com.minimall.common.auth.*;
import com.minimall.user.account.persistence.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MemberAccountDirectory implements AccountDirectory {
  private final MemberRepository repository;

  public MemberAccountDirectory(MemberRepository repository) {
    this.repository = repository;
  }

  public ActorType type() {
    return ActorType.MEMBER;
  }

  public Optional<AccountIdentity> findByLogin(String login) {
    return repository.login(login).map(this::identity);
  }

  public Optional<AccountIdentity> findById(long id) {
    return repository.find(id).map(this::identity);
  }

  private AccountIdentity identity(MemberEntity m) {
    return new AccountIdentity(
        type(), m.id, m.passwordHash, Boolean.TRUE.equals(m.enabled), m.authVersion, Set.of());
  }
}
