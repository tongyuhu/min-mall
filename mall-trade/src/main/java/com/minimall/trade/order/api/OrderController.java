package com.minimall.trade.order.api;

import com.minimall.common.api.*;
import com.minimall.common.auth.*;
import com.minimall.trade.order.application.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderService orders;
  private final CurrentActor actor;

  public OrderController(OrderService orders, CurrentActor actor) {
    this.orders = orders;
    this.actor = actor;
  }

  @PostMapping
  public OrderView place(
      @RequestHeader("Idempotency-Key") String key, @RequestBody PlaceOrderCommand command) {
    return orders.place(actor.require(ActorType.MEMBER), key, command);
  }

  @GetMapping("/{no}")
  public OrderView detail(@PathVariable String no) {
    return orders.detail(actor.require(ActorType.MEMBER), no);
  }

  @GetMapping
  public PageResult<OrderView> list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return orders.list(actor.require(ActorType.MEMBER), new PageQuery(page, size, "id"));
  }
}
