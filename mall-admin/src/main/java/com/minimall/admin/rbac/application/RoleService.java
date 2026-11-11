package com.minimall.admin.rbac.application;

import com.minimall.admin.auth.persistence.AdminRepository;
import com.minimall.common.api.BusinessException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
  private final AdminRepository repository;

  public RoleService(AdminRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void assign(long id, Set<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty() || roleIds.size() > 20)
      throw BusinessException.bad("角色不能为空且最多20项");
    repository.guard();
    var account = repository.lock(id);
    if (account.flag("enabled")
        && repository.superAdmin(id)
        && !roleIds.contains(1L)
        && repository.enabledSuperCount() <= 1) throw BusinessException.conflict("不能移除最后一个超级管理员");
    repository.roles(id, roleIds);
  }

  @Transactional
  public void updatePermissions(long roleId, Set<String> codes) {
    if (roleId == 1) throw BusinessException.conflict("超级管理员权限不可修改");
    if (codes == null || codes.size() > 100) throw BusinessException.bad("权限集合不合法");
    repository.guard();
    repository.permissions(roleId, codes);
  }

  public Set<String> permissions(long id) {
    return repository.permissions(id);
  }

  public List<RoleView> list() {
    return repository.roles().stream()
        .map(r -> new RoleView(Long.toString(r.number("id")), r.text("name")))
        .toList();
  }

  public record RoleView(String id, String name) {}
}
