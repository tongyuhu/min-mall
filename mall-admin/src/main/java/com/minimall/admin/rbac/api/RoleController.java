package com.minimall.admin.rbac.api;

import com.minimall.admin.auth.application.AdminAccountService;
import com.minimall.admin.rbac.application.RoleService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1")
@PreAuthorize("hasAuthority('rbac:write')")
public class RoleController {
  private final RoleService roles;
  private final AdminAccountService accounts;

  public RoleController(RoleService roles, AdminAccountService accounts) {
    this.roles = roles;
    this.accounts = accounts;
  }

  public record CreateAdmin(String username, String password, Set<Long> roleIds) {}

  public record RoleInput(Set<Long> roleIds) {}

  public record PermissionInput(Set<String> permissions) {}

  public record Enabled(boolean enabled) {}

  public record Password(String password) {}

  @GetMapping("/roles")
  public List<RoleService.RoleView> roles() {
    return roles.list();
  }

  @GetMapping("/administrators")
  public List<AdminAccountService.AdminView> list() {
    return accounts.list();
  }

  @PostMapping("/administrators")
  public Map<String, String> create(@RequestBody CreateAdmin input) {
    return Map.of(
        "id", Long.toString(accounts.create(input.username(), input.password(), input.roleIds())));
  }

  @PutMapping("/administrators/{id}/roles")
  public void assign(@PathVariable long id, @RequestBody RoleInput input) {
    roles.assign(id, input.roleIds());
  }

  @PutMapping("/roles/{id}/permissions")
  public void permissions(@PathVariable long id, @RequestBody PermissionInput input) {
    roles.updatePermissions(id, input.permissions());
  }

  @PutMapping("/administrators/{id}/enabled")
  public void enabled(@PathVariable long id, @RequestBody Enabled input) {
    accounts.setEnabled(id, input.enabled());
  }

  @PutMapping("/administrators/{id}/password")
  public void password(@PathVariable long id, @RequestBody Password input) {
    accounts.resetPassword(id, input.password());
  }
}
