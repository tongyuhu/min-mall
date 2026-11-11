package com.minimall.trade.payment.task;

import com.minimall.common.util.Json;
import com.minimall.trade.order.application.OrderCloseService;
import com.minimall.trade.order.persistence.OrderRepository;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.persistence.PaymentRepository;
import com.minimall.trade.task.application.*;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class QueryPaymentHandler implements TaskHandler {
  private final PaymentGateway gateway;
  private final PaymentRepository payments;
  private final OrderRepository orders;
  private final OrderCloseService closer;
  private final PaymentOutcomeService outcomes;
  private final Json json;
  private final Clock clock;

  public QueryPaymentHandler(
      PaymentGateway gateway,
      PaymentRepository payments,
      OrderRepository orders,
      OrderCloseService closer,
      PaymentOutcomeService outcomes,
      Json json,
      Clock clock) {
    this.gateway = gateway;
    this.payments = payments;
    this.orders = orders;
    this.closer = closer;
    this.outcomes = outcomes;
    this.json = json;
    this.clock = clock;
  }

  public String type() {
    return "QUERY_PAYMENT";
  }

  public TaskResult execute(TaskLease task) {
    String no = json.read(task.payload(), PaymentTaskCommand.class).paymentNo();
    var payment = payments.byNo(no, false);
    var result = gateway.query(no);
    if (result.status() == PaymentState.SUCCEEDED) {
      outcomes.observe(result);
      return result.paidAt() == null
          ? TaskResult.waitUntil(clock.instant().plusSeconds(5))
          : TaskResult.done();
    }
    if (result.status() == PaymentState.CLOSED) {
      closer.finalizeClose(payment.text("order_no"), result);
      return TaskResult.done();
    }
    var order = orders.byNo(payment.text("order_no"), false);
    if ("PENDING_PAYMENT".equals(order.text("status"))
        && !clock.instant().isBefore(order.instant("expires_at")))
      closer.request(order.number("member_id"), order.text("order_no"));
    return TaskResult.waitUntil(clock.instant().plusSeconds(5));
  }
}
