package com.minimall.trade.aftersale.api;

import com.minimall.common.auth.*;
import com.minimall.trade.aftersale.application.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/after-sales")
public class AfterSaleController {
  private final AfterSaleService service;
  private final CurrentActor actor;

  public AfterSaleController(AfterSaleService service, CurrentActor actor) {
    this.service = service;
    this.actor = actor;
  }

  public record Apply(String orderNo, String reason) {}

  public record Return(String carrier, String trackingNo) {}

  @PostMapping
  public AfterSaleView apply(@RequestBody Apply input) {
    return service.apply(actor.require(ActorType.MEMBER), input.orderNo(), input.reason());
  }

  @GetMapping
  public List<AfterSaleView> mine() {
    return service.mine(actor.require(ActorType.MEMBER));
  }

  @GetMapping("/{no}")
  public AfterSaleView detail(@PathVariable String no) {
    return service.detail(actor.require(ActorType.MEMBER), no);
  }

  @PostMapping("/{no}/return")
  public void returned(@PathVariable String no, @RequestBody Return input) {
    service.submitReturn(actor.require(ActorType.MEMBER), no, input.carrier(), input.trackingNo());
  }

  @PostMapping("/{no}/withdraw")
  public void withdraw(@PathVariable String no) {
    service.withdraw(actor.require(ActorType.MEMBER), no);
  }
}
