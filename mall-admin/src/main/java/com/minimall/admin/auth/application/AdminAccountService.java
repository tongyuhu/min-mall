package com.minimall.admin.auth.application;

import com.minimall.admin.auth.persistence.AdminRepository;
import com.minimall.admin.rbac.application.RoleService;
import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.PasswordRules;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {
  private final AdminRepository repository;
  private final RoleService roles;
  private final PasswordEncoder encoder;

  public AdminAccountService(
      AdminRepository repository, RoleService roles, PasswordEncoder encoder) {
    this.repository = repository;
    this.roles = roles;
    this.encoder = encoder;
  }

  @Transactional
  public long bootstrap(String username, String password) {
    repository.guard();
    var existing = repository.login(username);
    if (existing.isPresent()) return existing.get().number("id");
    if (repository.enabledSuperCount() > 0) throw BusinessException.conflict("已有超级管理员，不能再次初始化");
    PasswordRules.validate(password);
    long id = repository.insert(PasswordRules.login(username), encoder.encode(password));
    repository.roles(id, Set.of(1L));
    return id;
  }

  @Transactional
  public long create(String username, String password, Set<Long> roleIds) {
    PasswordRules.validate(password);
    repository.guard();
    long id = repository.insert(PasswordRules.login(username), encoder.encode(password));
    roles.assign(id, roleIds);
    return id;
  }

  @Transactional
  public void setEnabled(long id, boolean enabled) {
    repository.guard();
    var row = repository.lock(id);
    if (!enabled
        && row.flag("enabled")
        && repository.superAdmin(id)
        && repository.enabledSuperCount() <= 1) throw BusinessException.conflict("不能禁用最后一个超级管理员");
    repository.enabled(id, enabled);
  }

  @Transactional
  public void resetPassword(long id, String password) {
    PasswordRules.validate(password);
    repository.lock(id);
    repository.password(id, encoder.encode(password));
  }

  @Transactional
  public void changePassword(long id, String oldPassword, String password) {
    PasswordRules.validate(password);
    var row = repository.lock(id);
    if (oldPassword == null || !encoder.matches(oldPassword, row.text("password_hash")))
      throw BusinessException.bad("原密码不正确");
    repository.password(id, encoder.encode(password));
  }

  public List<AdminView> list() {
    return repository.accounts().stream()
        .map(
            r ->
                new AdminView(
                    Long.toString(r.number("id")),
                    r.text("username"),
                    r.flag("enabled"),
                    repository.roleIds(r.number("id"))))
        .toList();
  }

  public String username(long id) {
    return repository.find(id).orElseThrow(BusinessException::missing).text("username");
  }

  public record AdminView(String id, String username, boolean enabled, List<String> roleIds) {}
}
