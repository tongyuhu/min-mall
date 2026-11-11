package com.minimall.trade.payment.task;

import com.minimall.common.util.Json;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.task.application.*;
import org.springframework.stereotype.Component;

@Component
public class CreatePaymentHandler implements TaskHandler {
  private final PaymentService payments;
  private final PaymentGateway gateway;
  private final PaymentOutcomeService outcomes;
  private final Json json;

  public CreatePaymentHandler(
      PaymentService payments, PaymentGateway gateway, PaymentOutcomeService outcomes, Json json) {
    this.payments = payments;
    this.gateway = gateway;
    this.outcomes = outcomes;
    this.json = json;
  }

  public String type() {
    return "CREATE_PAYMENT";
  }

  public TaskResult execute(TaskLease task) {
    String no = json.read(task.payload(), PaymentTaskCommand.class).paymentNo();
    var request = payments.creationRequest(no);
    if (request.isEmpty()) return TaskResult.done();
    var existing = gateway.query(no);
    var result =
        existing.status() == PaymentState.UNKNOWN ? gateway.create(request.get()) : existing;
    payments.saveParameters(result);
    outcomes.observe(result);
    return TaskResult.done();
  }
}
