package com.minimall.admin.audit.api;

import com.minimall.admin.audit.application.AuditService;
import com.minimall.common.api.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/audits")
@PreAuthorize("hasAuthority('audit:read')")
public class AuditController {
  private final AuditService audit;

  public AuditController(AuditService audit) {
    this.audit = audit;
  }

  @GetMapping
  public PageResult<AuditService.AuditView> list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return audit.list(new PageQuery(page, size, "id"));
  }
}
