package com.minimall.admin.auth.api;

import com.minimall.user.account.application.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/members")
@PreAuthorize("hasAuthority('member:write')")
public class MemberAdminController {
  private final MemberService members;

  public MemberAdminController(MemberService members) {
    this.members = members;
  }

  public record Enabled(boolean enabled) {}

  @GetMapping
  public com.minimall.common.api.PageResult<MemberView> search(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return members.search(new com.minimall.common.api.PageQuery(page, size, "id"));
  }

  @GetMapping("/{id}")
  public MemberView detail(@PathVariable long id) {
    return members.profile(id);
  }

  @PutMapping("/{id}/enabled")
  public void enabled(@PathVariable long id, @RequestBody Enabled input) {
    members.setEnabled(id, input.enabled());
  }
}
