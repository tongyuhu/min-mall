package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderCloseRaceIT extends IntegrationTestSupport {
  @Autowired MallFixtures fixtures;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired PaymentOutcomeService outcomes;
  @Autowired OrderCloseService closer;
  @Autowired InventoryService stock;
  @Autowired IntegrationTaskService tasks;

  @Test
  void refundsAfterDeadlinePaymentBeforeLocalClose() {
    var buyer = fixtures.buyer();
    var goods = fixtures.goods(100, 1);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var o =
        orders.place(
            buyer.memberId(),
            fixtures.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var p = payments.create(buyer.memberId(), o.orderNo());
    outcomes.apply(
        new VerifiedPayment(
            "late" + fixtures.unique(),
            "mini-mall-local",
            p.paymentNo(),
            "channel" + fixtures.unique(),
            100,
            "CNY",
            o.expiresAt().plusSeconds(1),
            PaymentState.SUCCEEDED));
    assertEquals("CLOSED", orders.detail(buyer.memberId(), o.orderNo()).status());
    assertEquals(1, stock.get(goods.skuId()).available());
    assertEquals(
        1,
        tasks.pending().stream()
            .filter(
                t ->
                    t.type().equals("ENSURE_REFUND")
                        && t.businessKey().equals("LATE_PAYMENT:" + p.paymentNo()))
            .count());
  }

  @Test
  void unknownChannelDoesNotReleaseStock() {
    var buyer = fixtures.buyer();
    var goods = fixtures.goods(100, 1);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var o =
        orders.place(
            buyer.memberId(),
            fixtures.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var p = payments.create(buyer.memberId(), o.orderNo());
    closer.finalizeClose(
        o.orderNo(),
        new PaymentResult(p.paymentNo(), null, PaymentState.UNKNOWN, null, 0, "CNY", Map.of()));
    assertEquals("PENDING_PAYMENT", orders.detail(buyer.memberId(), o.orderNo()).status());
    assertEquals(1, stock.get(goods.skuId()).reserved());
  }
}
