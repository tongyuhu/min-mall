package com.minimall.trade.payment.task;

import com.minimall.common.util.Json;
import com.minimall.trade.order.application.OrderCloseService;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.persistence.PaymentRepository;
import com.minimall.trade.task.application.*;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class ClosePaymentHandler implements TaskHandler {
  private final PaymentGateway gateway;
  private final PaymentRepository payments;
  private final OrderCloseService closer;
  private final Json json;
  private final Clock clock;

  public ClosePaymentHandler(
      PaymentGateway gateway,
      PaymentRepository payments,
      OrderCloseService closer,
      Json json,
      Clock clock) {
    this.gateway = gateway;
    this.payments = payments;
    this.closer = closer;
    this.json = json;
    this.clock = clock;
  }

  public String type() {
    return "CLOSE_PAYMENT";
  }

  public TaskResult execute(TaskLease task) {
    String no = json.read(task.payload(), PaymentTaskCommand.class).paymentNo();
    var payment = payments.byNo(no, false);
    var result = gateway.query(no);
    if (result.status() != PaymentState.SUCCEEDED && result.status() != PaymentState.CLOSED)
      result = gateway.close(no);
    closer.finalizeClose(payment.text("order_no"), result);
    return result.status() == PaymentState.CLOSED
            || result.status() == PaymentState.SUCCEEDED && result.paidAt() != null
        ? TaskResult.done()
        : TaskResult.waitUntil(clock.instant().plusSeconds(5));
  }
}
