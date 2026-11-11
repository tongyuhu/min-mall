package com.minimall.trade.shipping.mock;

import com.minimall.trade.shipping.gateway.ShipmentTrack;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev/v1/shipments")
@Profile("(local | test) & !prod")
@ConditionalOnProperty(name = "mall.simulation.enabled", havingValue = "true")
public class MockShippingController {
  private final MockShippingGateway gateway;

  public MockShippingController(MockShippingGateway gateway) {
    this.gateway = gateway;
  }

  public record Failure(boolean enabled) {}

  @PostMapping("/{no}/tracks")
  public void append(@PathVariable String no, @RequestBody ShipmentTrack input) {
    gateway.append(no, input);
  }

  @PutMapping("/{no}/failure")
  public void fail(@PathVariable String no, @RequestBody Failure input) {
    gateway.failQueries(no, input.enabled());
  }
}
