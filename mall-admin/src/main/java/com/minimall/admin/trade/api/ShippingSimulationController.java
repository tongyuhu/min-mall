package com.minimall.admin.trade.api;

import com.minimall.trade.shipping.application.ShippingService;
import com.minimall.trade.shipping.gateway.ShipmentTrack;
import com.minimall.trade.shipping.mock.MockShippingGateway;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/simulation/shipments")
@Profile("(local | test) & !prod")
@ConditionalOnProperty(name = "mall.simulation.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('shipment:write')")
public class ShippingSimulationController {
  private final MockShippingGateway gateway;
  private final ShippingService shipping;

  public ShippingSimulationController(MockShippingGateway gateway, ShippingService shipping) {
    this.gateway = gateway;
    this.shipping = shipping;
  }

  @PostMapping("/{no}/delivered")
  public void delivered(@PathVariable String no) {
    gateway.append(
        no,
        new ShipmentTrack(UUID.randomUUID().toString(), Instant.now(), "DELIVERED", "本地模拟：包裹已送达"));
    shipping.requestSync(no);
  }
}
