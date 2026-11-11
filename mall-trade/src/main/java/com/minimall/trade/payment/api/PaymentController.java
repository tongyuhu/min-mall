package com.minimall.trade.payment.api;

import com.minimall.common.auth.*;
import com.minimall.trade.payment.application.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
  private final PaymentService payments;
  private final CurrentActor actor;

  public PaymentController(PaymentService payments, CurrentActor actor) {
    this.payments = payments;
    this.actor = actor;
  }

  public record Create(String orderNo) {}

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public PaymentView create(@RequestBody Create input) {
    return payments.create(actor.require(ActorType.MEMBER), input.orderNo());
  }

  @GetMapping("/{no}")
  public PaymentView get(@PathVariable String no) {
    return payments.get(actor.require(ActorType.MEMBER), no);
  }
}
