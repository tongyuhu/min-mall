package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.shipping.application.*;
import com.minimall.trade.shipping.gateway.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ShippingIT extends IntegrationTestSupport {
  @Autowired MallFixtures f;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired ShippingService shipping;

  @Test
  void doesNotConfirmOrderOnCarrierDelivery() {
    var buyer = f.buyer();
    var goods = f.goods(0, 1);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var order =
        orders.place(
            buyer.memberId(),
            f.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var shipment = shipping.ship(order.orderNo(), "模拟快递", f.unique());
    assertEquals(
        shipment.shipmentNo(),
        shipping.ship(order.orderNo(), "模拟快递", shipment.trackingNo()).shipmentNo());
    var track = new ShipmentTrack("e" + f.unique(), Instant.now(), "DELIVERED", "已签收");
    shipping.applyTracks(shipment.shipmentNo(), List.of(track, track));
    assertEquals("SHIPPED", orders.detail(buyer.memberId(), order.orderNo()).status());
    shipping.confirmReceipt(buyer.memberId(), order.orderNo());
    assertEquals("COMPLETED", orders.detail(buyer.memberId(), order.orderNo()).status());
  }
}
