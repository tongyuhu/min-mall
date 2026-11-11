package com.minimall.trade.shipping.task;

import com.minimall.common.util.Json;
import com.minimall.trade.shipping.application.*;
import com.minimall.trade.shipping.gateway.ShippingGateway;
import com.minimall.trade.task.application.*;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class CreateShipmentHandler implements TaskHandler {
  private final ShippingService shipping;
  private final ShippingGateway gateway;
  private final Json json;
  private final Clock clock;

  public CreateShipmentHandler(
      ShippingService shipping, ShippingGateway gateway, Json json, Clock clock) {
    this.shipping = shipping;
    this.gateway = gateway;
    this.json = json;
    this.clock = clock;
  }

  public String type() {
    return "CREATE_SHIPMENT";
  }

  public TaskResult execute(TaskLease lease) {
    String no = json.read(lease.payload(), ShipmentTaskCommand.class).shipmentNo();
    shipping.accept(gateway.create(shipping.creationRequest(no)));
    return TaskResult.done();
  }
}
