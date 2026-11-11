package com.minimall.trade.payment.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.util.*;
import com.minimall.trade.order.application.OrderSettlementService;
import com.minimall.trade.order.persistence.OrderRepository;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.persistence.PaymentRepository;
import com.minimall.trade.refund.application.EnsureRefundCommand;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOutcomeService {
  private final PaymentRepository payments;
  private final OrderRepository orders;
  private final OrderSettlementService settlement;
  private final IntegrationTaskService tasks;
  private final Json json;
  private final String merchant;

  public PaymentOutcomeService(
      PaymentRepository payments,
      OrderRepository orders,
      OrderSettlementService settlement,
      IntegrationTaskService tasks,
      Json json,
      @Value("${mall.simulation.merchant-id}") String merchant) {
    this.payments = payments;
    this.orders = orders;
    this.settlement = settlement;
    this.tasks = tasks;
    this.json = json;
    this.merchant = merchant;
  }

  @Transactional
  public void apply(VerifiedPayment p) {
    if (p == null
        || p.paymentNo() == null
        || p.notificationId() == null
        || p.notificationId().length() > 64
        || p.channelTransactionNo() == null
        || p.channelTransactionNo().length() > 64
        || p.status() == null) throw BusinessException.bad("通知参数不合法");
    var initial = payments.byNo(p.paymentNo(), false);
    var order = orders.byNo(initial.text("order_no"), true);
    var payment = payments.byNo(p.paymentNo(), true);
    if (!merchant.equals(p.merchantId())
        || !payment.text("currency").equals(p.currency())
        || payment.number("amount_fen") != p.amountFen())
      throw BusinessException.bad("支付金额或商户信息不匹配");
    if (payment.text("channel_transaction_no") != null
        && !payment.text("channel_transaction_no").equals(p.channelTransactionNo()))
      throw BusinessException.conflict("渠道交易号不一致");
    String hash = Digests.sha256(json.write(p));
    var notification = payments.notification(p.notificationId());
    if (notification.isPresent()) {
      if (!notification.get().text("payload_hash").equals(hash)
          || !notification.get().text("payment_no").equals(p.paymentNo()))
        throw BusinessException.conflict("通知编号内容冲突");
      return;
    }
    if (p.status() == PaymentState.FAILED) {
      payments.notification(p.notificationId(), p.paymentNo(), hash);
      payments.failed(p.paymentNo());
      return;
    }
    if (p.status() != PaymentState.SUCCEEDED) return;
    if (p.paidAt() == null) {
      tasks.enqueue(
          "QUERY_PAYMENT",
          "uncertain:" + p.notificationId(),
          json.write(new PaymentTaskCommand(p.paymentNo())));
      return;
    }
    payments.notification(p.notificationId(), p.paymentNo(), hash);
    if ("SUCCEEDED".equals(payment.text("status"))) return;
    payments.success(p);
    String no = order.text("order_no");
    if ("CLOSED".equals(order.text("status")) || p.paidAt().isAfter(order.instant("expires_at"))) {
      if ("PENDING_PAYMENT".equals(order.text("status"))) settlement.close(no, "超过支付期限收款，自动退款");
      String key = "LATE_PAYMENT:" + p.paymentNo();
      tasks.enqueue(
          "ENSURE_REFUND", key, json.write(new EnsureRefundCommand(key, no, null, "已关闭或超时订单异常收款")));
      return;
    }
    Checks.state("PENDING_PAYMENT".equals(order.text("status")), "订单状态与支付不一致");
    settlement.paid(no, p.paidAt());
  }

  @Transactional
  public void observe(PaymentResult result) {
    if (result.status() != PaymentState.SUCCEEDED && result.status() != PaymentState.FAILED) return;
    String id = "Q" + Digests.sha256(json.write(result)).substring(0, 40);
    apply(
        new VerifiedPayment(
            id,
            merchant,
            result.paymentNo(),
            result.channelTransactionNo(),
            result.amountFen(),
            result.currency(),
            result.paidAt(),
            result.status()));
  }
}
