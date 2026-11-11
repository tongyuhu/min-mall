package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.common.util.Json;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.mock.*;
import com.minimall.trade.payment.task.CreatePaymentHandler;
import com.minimall.trade.task.application.*;
import com.minimall.trade.task.infrastructure.IntegrationTaskWorker;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class PaymentRecoveryIT extends IntegrationTestSupport {
  @Autowired MallFixtures f;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired PaymentOutcomeService outcomes;
  @Autowired MockPaymentGateway gateway;
  @Autowired MockNotificationHandler notifications;
  @Autowired IntegrationTaskService tasks;
  @Autowired InventoryService stock;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired Json json;
  @Autowired PlatformTransactionManager manager;

  record Purchase(
      MallFixtures.Buyer buyer, MallFixtures.Goods goods, OrderView order, PaymentView payment) {}

  Purchase purchase() {
    var buyer = f.buyer();
    var goods = f.goods(100, 2);
    var c =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var order =
        orders.place(
            buyer.memberId(),
            f.unique(),
            new PlaceOrderCommand(c, checkout.preview(buyer.memberId(), c).fingerprint(), false));
    var p = payments.create(buyer.memberId(), order.orderNo());
    gateway.create(
        new PaymentRequest(p.paymentNo(), order.orderNo(), 100, "CNY", order.expiresAt()));
    return new Purchase(buyer, goods, order, p);
  }

  @Test
  void failedNotificationKeepsReservationAndSamePaymentCanSucceed() {
    var p = purchase();
    String no = p.payment().paymentNo();
    var worker = new IntegrationTaskWorker(tasks, List.of(notifications), clock, false);
    assertEquals(PaymentState.FAILED, gateway.fail(no).status());
    assertEquals("PENDING", payments.get(p.buyer().memberId(), no).status());
    for (int i = 0;
        i < 10 && !"FAILED".equals(payments.get(p.buyer().memberId(), no).status());
        i++) worker.runOnce();
    assertEquals("FAILED", payments.get(p.buyer().memberId(), no).status());
    assertEquals(
        "PENDING_PAYMENT", orders.detail(p.buyer().memberId(), p.order().orderNo()).status());
    assertEquals(new StockView(1, 1), stock.get(p.goods().skuId()));
    assertEquals(no, payments.create(p.buyer().memberId(), p.order().orderNo()).paymentNo());
    var success = gateway.succeed(no, null, false, 2, 0, 0);
    for (int i = 0;
        i < 10 && !"SUCCEEDED".equals(payments.get(p.buyer().memberId(), no).status());
        i++) worker.runOnce();
    assertEquals("PAID", orders.detail(p.buyer().memberId(), p.order().orderNo()).status());
    // 不同通知号重复同一次收款，随后迟到的失败也不能倒退成功终态。
    for (int i = 0; i < 2; i++)
      outcomes.apply(
          new VerifiedPayment(
              f.unique(),
              "mini-mall-local",
              no,
              success.channelTransactionNo(),
              100,
              "CNY",
              success.paidAt(),
              PaymentState.SUCCEEDED));
    outcomes.apply(
        new VerifiedPayment(
            f.unique(),
            "mini-mall-local",
            no,
            success.channelTransactionNo(),
            100,
            "CNY",
            null,
            PaymentState.FAILED));
    assertEquals("SUCCEEDED", payments.get(p.buyer().memberId(), no).status());
    assertEquals(new StockView(1, 0), stock.get(p.goods().skuId()));
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM stock_log WHERE business_type='COMMIT' AND business_no=?",
            Long.class,
            p.order().orderNo()));
    assertThrows(BusinessException.class, () -> gateway.fail(no));
  }

  @Test
  void expiredWorkerCannotRegressPaymentAfterItsDelayedResponse() throws Exception {
    var p = purchase();
    String no = p.payment().paymentNo();
    var pending = gateway.query(no);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    PaymentGateway delayed =
        new PaymentGateway() {
          public PaymentResult create(PaymentRequest r) {
            return gateway.create(r);
          }

          public PaymentResult close(String n) {
            return gateway.close(n);
          }

          public PaymentResult query(String n) {
            entered.countDown();
            try {
              if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("等待测试信号超时");
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(e);
            }
            return pending;
          }

          public RefundResult refund(RefundRequest r) {
            return gateway.refund(r);
          }

          public RefundResult queryRefund(String n) {
            return gateway.queryRefund(n);
          }

          public VerifiedPayment verify(byte[] b, Map<String, String> h) {
            return gateway.verify(b, h);
          }
        };
    var handler = new CreatePaymentHandler(payments, delayed, outcomes, json);
    String type = "TEST_" + f.unique();
    long id =
        new TransactionTemplate(manager)
            .execute(s -> tasks.enqueue(type, no, json.write(new PaymentTaskCommand(no))));
    var old = tasks.claim(Set.of(type), clock.instant(), 1, Duration.ofSeconds(60)).getFirst();
    try (var pool = Executors.newSingleThreadExecutor()) {
      var future = pool.submit(() -> handler.execute(old));
      try {
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        jdbc.update(
            "UPDATE integration_task SET lease_until=? WHERE id=?",
            java.time.LocalDateTime.ofInstant(
                clock.instant().minusSeconds(1), java.time.ZoneOffset.UTC),
            id);
        var replacement =
            tasks.claim(Set.of(type), clock.instant(), 1, Duration.ofSeconds(60)).getFirst();
        outcomes.observe(gateway.succeed(no, null, false, 1, 3600, 0));
        assertTrue(tasks.complete(id, replacement.leaseToken()));
      } finally {
        release.countDown();
      }
      assertEquals(TaskDisposition.DONE, future.get(10, TimeUnit.SECONDS).disposition());
      assertFalse(tasks.complete(id, old.leaseToken()));
    }
    assertEquals("SUCCEEDED", payments.get(p.buyer().memberId(), no).status());
    assertEquals("PAID", orders.detail(p.buyer().memberId(), p.order().orderNo()).status());
    assertEquals(new StockView(1, 0), stock.get(p.goods().skuId()));
  }
}
