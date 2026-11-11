package com.minimall.admin.catalog.api;

import com.minimall.catalog.stock.application.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/stock")
@PreAuthorize("hasAuthority('stock:adjust')")
public class StockAdminController {
  private final InventoryService inventory;

  public StockAdminController(InventoryService inventory) {
    this.inventory = inventory;
  }

  public record Adjustment(int delta) {}

  @GetMapping("/{skuId}")
  public StockView get(@PathVariable long skuId) {
    return inventory.get(skuId);
  }

  @PostMapping("/{skuId}/adjustments")
  public void adjust(
      @PathVariable long skuId,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody Adjustment input) {
    inventory.adjust(skuId, input.delta(), key);
  }
}
