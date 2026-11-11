package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentNotificationIT extends IntegrationTestSupport {
  @Autowired MallFixtures fixtures;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired PaymentOutcomeService outcomes;
  @Autowired InventoryService stock;
  @Autowired Clock clock;

  @Test
  void doesNotRegressSuccessOnLateCreateResponse() {
    var buyer = fixtures.buyer();
    var goods = fixtures.goods(100, 5);
    var command =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var order =
        orders.place(
            buyer.memberId(),
            fixtures.unique(),
            new PlaceOrderCommand(
                command, checkout.preview(buyer.memberId(), command).fingerprint(), false));
    var payment = payments.create(buyer.memberId(), order.orderNo());
    var success =
        new VerifiedPayment(
            "n" + fixtures.unique(),
            "mini-mall-local",
            payment.paymentNo(),
            "channel" + fixtures.unique(),
            100,
            "CNY",
            clock.instant(),
            PaymentState.SUCCEEDED);
    outcomes.apply(success);
    outcomes.apply(success);
    payments.saveParameters(
        new PaymentResult(
            payment.paymentNo(),
            success.channelTransactionNo(),
            PaymentState.PENDING,
            null,
            100,
            "CNY",
            Map.of()));
    assertEquals("SUCCEEDED", payments.get(buyer.memberId(), payment.paymentNo()).status());
    assertEquals("PAID", orders.detail(buyer.memberId(), order.orderNo()).status());
    assertEquals(new StockView(4, 0), stock.get(goods.skuId()));
    assertThrows(
        BusinessException.class,
        () ->
            outcomes.apply(
                new VerifiedPayment(
                    "bad" + fixtures.unique(),
                    "mini-mall-local",
                    payment.paymentNo(),
                    success.channelTransactionNo(),
                    1,
                    "CNY",
                    success.paidAt(),
                    PaymentState.SUCCEEDED)));
  }
}
