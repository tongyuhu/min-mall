package com.minimall.trade.shipping.api;

import com.minimall.common.auth.*;
import com.minimall.trade.shipping.application.ShippingService;
import com.minimall.trade.shipping.gateway.ShipmentTrack;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class ShippingController {
  private final ShippingService shipping;
  private final CurrentActor actor;

  public ShippingController(ShippingService shipping, CurrentActor actor) {
    this.shipping = shipping;
    this.actor = actor;
  }

  @GetMapping("/{no}/tracks")
  public List<ShipmentTrack> tracks(@PathVariable String no) {
    return shipping.tracks(actor.require(ActorType.MEMBER), no);
  }

  @PostMapping("/{no}/receipt")
  public void receipt(@PathVariable String no) {
    shipping.confirmReceipt(actor.require(ActorType.MEMBER), no);
  }
}
