package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ExpiryScanIT extends IntegrationTestSupport {
  @Autowired MallFixtures f;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired OrderCloseService closer;
  @Autowired JdbcTemplate jdbc;
  @Autowired InventoryService stock;

  @Test
  void previouslyRequestedCloseDoesNotStarveTheNextExpiredOrder() {
    var buyer = f.buyer();
    var goods = f.goods(100, 3);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var place =
        new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false);
    var first = orders.place(buyer.memberId(), f.unique(), place);
    var next = orders.place(buyer.memberId(), f.unique(), place);
    // 仅本用例两单移到极早截止时间，第一单模拟已经可靠提交关闭请求但渠道未知。
    Instant firstExpiry = Instant.parse("2000-01-01T00:00:00Z");
    Instant nextExpiry = firstExpiry.plusSeconds(1);
    jdbc.update(
        "UPDATE trade_order SET expires_at=?,close_requested=1 WHERE order_no=?",
        java.time.LocalDateTime.ofInstant(firstExpiry, java.time.ZoneOffset.UTC),
        first.orderNo());
    jdbc.update(
        "UPDATE trade_order SET expires_at=? WHERE order_no=?",
        java.time.LocalDateTime.ofInstant(nextExpiry, java.time.ZoneOffset.UTC),
        next.orderNo());
    assertEquals(1, closer.requestExpired(nextExpiry.plusSeconds(1), 1));
    assertEquals("CLOSED", orders.detail(buyer.memberId(), next.orderNo()).status());
    assertEquals("PENDING_PAYMENT", orders.detail(buyer.memberId(), first.orderNo()).status());
    assertEquals(2, stock.get(goods.skuId()).available());
    assertEquals(1, stock.get(goods.skuId()).reserved());
  }
}
