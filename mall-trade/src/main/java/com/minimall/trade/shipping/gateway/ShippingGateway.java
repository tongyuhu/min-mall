package com.minimall.trade.shipping.gateway;

import java.util.List;

public interface ShippingGateway {
  ShipmentResult create(ShipmentRequest request);

  List<ShipmentTrack> query(String shipmentNo);
}
