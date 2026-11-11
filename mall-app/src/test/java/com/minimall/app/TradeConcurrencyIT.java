package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.trade.aftersale.application.*;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.shipping.application.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TradeConcurrencyIT extends IntegrationTestSupport {
  @Autowired MallFixtures f;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired InventoryService stock;
  @Autowired AfterSaleService aftersales;
  @Autowired ShippingService shipping;

  record Attempt(Object result, BusinessException conflict) {}

  List<Attempt> race(Callable<?> first, Callable<?> second) throws Exception {
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<Future<Attempt>>();
      for (var call : List.of(first, second))
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    return new Attempt(call.call(), null);
                  } catch (BusinessException e) {
                    return new Attempt(null, e);
                  }
                }));
      start.countDown();
      return List.of(
          futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS));
    }
  }

  PlaceOrderCommand command(MallFixtures.Buyer buyer, long sku, int quantity) {
    var c = new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(sku, quantity)), null);
    return new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false);
  }

  @Test
  void concurrentSameKeyAndPayloadCreatesOneOrder() throws Exception {
    var buyer = f.buyer();
    var goods = f.goods(100, 5);
    var c = command(buyer, goods.skuId(), 1);
    String key = f.unique();
    var result =
        race(
            () -> orders.place(buyer.memberId(), key, c),
            () -> orders.place(buyer.memberId(), key, c));
    assertNull(result.get(0).conflict());
    assertNull(result.get(1).conflict());
    assertEquals(
        ((OrderView) result.get(0).result()).orderNo(),
        ((OrderView) result.get(1).result()).orderNo());
    assertEquals(new StockView(4, 1), stock.get(goods.skuId()));
  }

  @Test
  void concurrentSameKeyWithDifferentPayloadRejectsOne() throws Exception {
    var buyer = f.buyer();
    var goods = f.goods(100, 5);
    var a = command(buyer, goods.skuId(), 1);
    var b = command(buyer, goods.skuId(), 2);
    String key = f.unique();
    var result =
        race(
            () -> orders.place(buyer.memberId(), key, a),
            () -> orders.place(buyer.memberId(), key, b));
    assertEquals(1, result.stream().filter(r -> r.conflict() != null).count());
    var rejected = result.stream().filter(r -> r.conflict() != null).findFirst().orElseThrow();
    assertEquals(409, rejected.conflict().httpStatus());
    var order =
        (OrderView)
            result.stream().filter(r -> r.result() != null).findFirst().orElseThrow().result();
    int quantity = order.items().getFirst().quantity();
    assertEquals(new StockView(5 - quantity, quantity), stock.get(goods.skuId()));
  }

  @Test
  void applyingAfterSaleAndShippingHaveAValidSerialOutcome() throws Exception {
    var buyer = f.buyer();
    var goods = f.goods(0, 1);
    var order = orders.place(buyer.memberId(), f.unique(), command(buyer, goods.skuId(), 1));
    race(
        () -> aftersales.apply(buyer.memberId(), order.orderNo(), "并发申请"),
        () -> shipping.ship(order.orderNo(), "模拟快递", f.unique()));
    var current = orders.detail(buyer.memberId(), order.orderNo());
    var after = aftersales.detail(buyer.memberId(), current.activeAfterSaleNo());
    assertEquals("WAIT_REVIEW", after.status());
    if (current.status().equals("PAID")) {
      assertEquals("REFUND_ONLY", after.type());
      assertTrue(shipping.byOrder(order.orderNo()).isEmpty());
    } else {
      assertEquals("SHIPPED", current.status());
      assertEquals("RETURN_REFUND", after.type());
      assertTrue(shipping.byOrder(order.orderNo()).isPresent());
    }
  }

  @Test
  void applyingAfterSaleAndReceiptHaveAValidSerialOutcome() throws Exception {
    var buyer = f.buyer();
    var goods = f.goods(0, 1);
    var order = orders.place(buyer.memberId(), f.unique(), command(buyer, goods.skuId(), 1));
    shipping.ship(order.orderNo(), "模拟快递", f.unique());
    race(
        () -> aftersales.apply(buyer.memberId(), order.orderNo(), "并发售后"),
        () -> {
          shipping.confirmReceipt(buyer.memberId(), order.orderNo());
          return true;
        });
    var current = orders.detail(buyer.memberId(), order.orderNo());
    assertTrue(Set.of("SHIPPED", "COMPLETED").contains(current.status()));
    assertEquals(
        "RETURN_REFUND", aftersales.detail(buyer.memberId(), current.activeAfterSaleNo()).type());
    assertEquals(1, aftersales.mine(buyer.memberId()).size());
    assertEquals(new StockView(0, 0), stock.get(goods.skuId()));
  }
}
