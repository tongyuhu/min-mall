package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlaceOrderIT extends IntegrationTestSupport {
  @Autowired MallFixtures fixtures;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired InventoryService stock;

  @Test
  void deduplicatesPlacementAndRejectsChangedPayload() {
    var buyer = fixtures.buyer();
    var goods = fixtures.goods(100, 5);
    var input =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 2)), null);
    var quote = checkout.preview(buyer.memberId(), input);
    var command = new PlaceOrderCommand(input, quote.fingerprint(), false);
    String key = fixtures.unique();
    var first = orders.place(buyer.memberId(), key, command);
    assertEquals(first.orderNo(), orders.place(buyer.memberId(), key, command).orderNo());
    assertEquals(new StockView(3, 2), stock.get(goods.skuId()));
    assertThrows(
        BusinessException.class,
        () ->
            orders.place(buyer.memberId(), key, new PlaceOrderCommand(input, "different", false)));
  }

  @Test
  void rollsBackFailedOrderAndSettlesZeroValue() {
    var buyer = fixtures.buyer();
    var empty = fixtures.goods(100, 0);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(empty.skuId(), 1)), null);
    var q = checkout.preview(buyer.memberId(), c);
    assertThrows(
        BusinessException.class,
        () ->
            orders.place(
                buyer.memberId(),
                fixtures.unique(),
                new PlaceOrderCommand(c, q.fingerprint(), false)));
    var free = fixtures.goods(0, 1);
    var freeCommand =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(free.skuId(), 1)), null);
    var freeQuote = checkout.preview(buyer.memberId(), freeCommand);
    var order =
        orders.place(
            buyer.memberId(),
            fixtures.unique(),
            new PlaceOrderCommand(freeCommand, freeQuote.fingerprint(), false));
    assertEquals("PAID", order.status());
    assertEquals(new StockView(0, 0), stock.get(free.skuId()));
  }
}
