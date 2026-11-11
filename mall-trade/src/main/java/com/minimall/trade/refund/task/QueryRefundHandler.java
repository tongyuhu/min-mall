package com.minimall.trade.refund.task;

import com.minimall.common.util.Json;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.refund.application.*;
import com.minimall.trade.task.application.*;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class QueryRefundHandler implements TaskHandler {
  private final RefundService refunds;
  private final PaymentGateway gateway;
  private final Json json;
  private final Clock clock;

  public QueryRefundHandler(RefundService refunds, PaymentGateway gateway, Json json, Clock clock) {
    this.refunds = refunds;
    this.gateway = gateway;
    this.json = json;
    this.clock = clock;
  }

  public String type() {
    return "QUERY_REFUND";
  }

  public TaskResult execute(TaskLease lease) {
    String no = json.read(lease.payload(), RefundTaskCommand.class).refundNo();
    if ("SUCCEEDED".equals(refunds.internal(no).status())) return TaskResult.done();
    var result = gateway.queryRefund(no);
    if (result.status() == RefundState.FAILED) result = gateway.refund(refunds.request(no));
    refunds.applyResult(no, result);
    return switch (result.status()) {
      case SUCCEEDED -> TaskResult.done();
      case FAILED -> TaskResult.retry("退款重试失败");
      default -> TaskResult.waitUntil(clock.instant().plusSeconds(10));
    };
  }
}
