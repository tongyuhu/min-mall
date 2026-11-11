package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.mock.MockPaymentGateway;
import com.minimall.trade.refund.application.RefundService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RefundRecoveryIT extends IntegrationTestSupport {
  @Autowired com.minimall.trade.task.application.IntegrationTaskService tasks;
  @Autowired com.minimall.trade.task.infrastructure.IntegrationTaskWorker worker;

  @Test
  void manualRefundRecoveryReusesTheOriginalRefundNumber() {
    var buyer = f.buyer();
    var goods = f.goods(100, 1);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var o =
        orders.place(
            buyer.memberId(),
            f.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var p = payments.create(buyer.memberId(), o.orderNo());
    gateway.create(new PaymentRequest(p.paymentNo(), o.orderNo(), 100, "CNY", o.expiresAt()));
    outcomes.observe(
        gateway.succeed(p.paymentNo(), o.expiresAt().plusSeconds(1), true, 1, 3600, 0));
    String no = refunds.ensure("LATE_PAYMENT:" + p.paymentNo(), o.orderNo(), null, "异常收款退款");
    gateway.refundMode(p.paymentNo(), "FAIL");
    refunds.applyResult(no, gateway.refund(refunds.request(no)));
    assertEquals("FAILED", refunds.internal(no).status());
    // 将已持久化任务置为人工态，模拟自动重试耗尽；不改订单、退款号或资金状态。
    long id =
        jdbc.queryForObject(
            "SELECT id FROM integration_task WHERE type='EXECUTE_REFUND' AND business_key=?",
            Long.class,
            no);
    jdbc.update("UPDATE integration_task SET status='MANUAL',attempts=6 WHERE id=?", id);
    gateway.refundMode(p.paymentNo(), "SUCCESS");
    tasks.retryManually(id);
    for (int i = 0; i < 10 && !"SUCCEEDED".equals(refunds.internal(no).status()); i++)
      worker.runOnce();
    assertEquals("SUCCEEDED", refunds.internal(no).status());
    assertEquals(no, refunds.get(buyer.memberId(), no).refundNo());
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM mock_refund_transaction WHERE payment_no=?",
            Long.class,
            p.paymentNo()));
    assertEquals(1, stock.get(goods.skuId()).available());
    assertThrows(
        com.minimall.common.api.BusinessException.class, () -> gateway.resolveRefund(no, false));
  }

  @Autowired MallFixtures f;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired PaymentOutcomeService outcomes;
  @Autowired MockPaymentGateway gateway;
  @Autowired RefundService refunds;
  @Autowired InventoryService stock;
  @Autowired JdbcTemplate jdbc;

  @Test
  void refundsClosedOrderWithoutRestockingTwice() {
    var buyer = f.buyer();
    var goods = f.goods(100, 1);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var o =
        orders.place(
            buyer.memberId(),
            f.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var p = payments.create(buyer.memberId(), o.orderNo());
    gateway.create(new PaymentRequest(p.paymentNo(), o.orderNo(), 100, "CNY", o.expiresAt()));
    outcomes.observe(
        gateway.succeed(p.paymentNo(), o.expiresAt().plusSeconds(1), true, 1, 3600, 0));
    String no = refunds.ensure("LATE_PAYMENT:" + p.paymentNo(), o.orderNo(), null, "已关闭或超时订单异常收款");
    var result = gateway.refund(refunds.request(no));
    refunds.applyResult(no, result);
    refunds.applyResult(no, result);
    assertEquals("CLOSED", orders.detail(buyer.memberId(), o.orderNo()).status());
    assertEquals(1, stock.get(goods.skuId()).available());
    assertEquals(
        0L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM stock_log WHERE business_type='RESTOCK' AND business_no=?",
            Long.class,
            no));
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM mock_refund_transaction WHERE payment_no=?",
            Long.class,
            p.paymentNo()));
  }
}
