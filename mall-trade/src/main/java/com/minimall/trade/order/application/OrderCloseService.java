package com.minimall.trade.order.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.util.*;
import com.minimall.trade.order.persistence.OrderRepository;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.persistence.PaymentRepository;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderCloseService {
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final PaymentOutcomeService outcomes;
  private final OrderSettlementService settlement;
  private final IntegrationTaskService tasks;
  private final Json json;
  private final TransactionTemplate tx;

  public OrderCloseService(
      OrderRepository orders,
      PaymentRepository payments,
      PaymentOutcomeService outcomes,
      OrderSettlementService settlement,
      IntegrationTaskService tasks,
      Json json,
      PlatformTransactionManager manager) {
    this.orders = orders;
    this.payments = payments;
    this.outcomes = outcomes;
    this.settlement = settlement;
    this.tasks = tasks;
    this.json = json;
    this.tx = new TransactionTemplate(manager);
  }

  @Transactional
  public void request(long member, String no) {
    var order = orders.byNo(no, true);
    if (order.number("member_id") != member) throw BusinessException.missing();
    if ("CLOSED".equals(order.text("status"))) return;
    Checks.state("PENDING_PAYMENT".equals(order.text("status")), "已支付订单请申请售后");
    begin(no);
  }

  public int requestExpired(Instant now, int limit) {
    int count = 0;
    for (var row : orders.expired(now, Math.min(100, Math.max(1, limit)))) {
      Boolean started =
          tx.execute(
              s -> {
                var current = orders.byNo(row.text("order_no"), true);
                if (!"PENDING_PAYMENT".equals(current.text("status"))
                    || current.instant("expires_at").isAfter(now)) return false;
                begin(row.text("order_no"));
                return true;
              });
      if (Boolean.TRUE.equals(started)) count++;
    }
    return count;
  }

  private void begin(String no) {
    orders.closeRequested(no);
    var payment = payments.byOrder(no);
    if (payment.isEmpty()) {
      settlement.close(no, "用户取消或支付超时");
      return;
    }
    String paymentNo = payment.get().text("payment_no");
    payments.byNo(paymentNo, true);
    tasks.enqueue("CLOSE_PAYMENT", paymentNo, json.write(new PaymentTaskCommand(paymentNo)));
  }

  @Transactional
  public void finalizeClose(String no, PaymentResult result) {
    var order = orders.byNo(no, true);
    var payment = payments.byOrder(no).orElseThrow(BusinessException::missing);
    if (!payment.text("payment_no").equals(result.paymentNo()))
      throw BusinessException.conflict("关闭结果不属于该订单");
    payments.byNo(result.paymentNo(), true);
    if (result.status() == PaymentState.SUCCEEDED) {
      outcomes.observe(result);
      return;
    }
    if (result.status() != PaymentState.CLOSED) return;
    if ("PENDING_PAYMENT".equals(order.text("status"))) {
      Checks.state(
          !"SUCCEEDED".equals(payments.byNo(result.paymentNo(), false).text("status")),
          "支付结果存在冲突，需要查单");
      settlement.close(no, "渠道已确认关闭");
      payments.closed(result.paymentNo());
    }
  }
}
