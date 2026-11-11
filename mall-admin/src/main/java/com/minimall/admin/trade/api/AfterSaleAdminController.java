package com.minimall.admin.trade.api;

import com.minimall.common.api.*;
import com.minimall.trade.aftersale.application.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/after-sales")
@PreAuthorize("hasAuthority('aftersale:review')")
public class AfterSaleAdminController {
  private final AfterSaleService service;

  public AfterSaleAdminController(AfterSaleService service) {
    this.service = service;
  }

  public record Review(boolean approved, String reason) {}

  @GetMapping
  public PageResult<AfterSaleView> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.search(status, new PageQuery(page, size, "id"));
  }

  @PostMapping("/{no}/review")
  public void review(@PathVariable String no, @RequestBody Review input) {
    service.review(no, input.approved(), input.reason());
  }

  @PostMapping("/{no}/accept-return")
  public void accept(@PathVariable String no) {
    service.acceptReturn(no);
  }
}
