package com.minimall.trade.refund.api;

import com.minimall.common.auth.*;
import com.minimall.trade.refund.application.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RefundController {
  private final RefundService refunds;
  private final CurrentActor actor;

  public RefundController(RefundService refunds, CurrentActor actor) {
    this.refunds = refunds;
    this.actor = actor;
  }

  @GetMapping("/refunds/{no}")
  public RefundView get(@PathVariable String no) {
    return refunds.get(actor.require(ActorType.MEMBER), no);
  }

  @GetMapping("/orders/{no}/refunds")
  public List<RefundView> order(@PathVariable String no) {
    return refunds.byOrder(actor.require(ActorType.MEMBER), no);
  }
}
