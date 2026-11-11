package com.minimall.admin.auth.application;

import com.minimall.admin.auth.persistence.AdminRepository;
import com.minimall.common.auth.*;
import com.minimall.common.persistence.DbRow;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountDirectory implements AccountDirectory {
  private final AdminRepository repository;

  public AdminAccountDirectory(AdminRepository repository) {
    this.repository = repository;
  }

  public ActorType type() {
    return ActorType.ADMIN;
  }

  public Optional<AccountIdentity> findByLogin(String login) {
    return repository.login(login).map(this::identity);
  }

  public Optional<AccountIdentity> findById(long id) {
    return repository.find(id).map(this::identity);
  }

  private AccountIdentity identity(DbRow row) {
    long id = row.number("id");
    return new AccountIdentity(
        type(),
        id,
        row.text("password_hash"),
        row.flag("enabled"),
        row.number("auth_version"),
        repository.permissions(id));
  }
}
