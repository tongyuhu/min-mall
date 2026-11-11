package com.minimall.trade.refund.task;

import com.minimall.common.util.Json;
import com.minimall.trade.refund.application.*;
import com.minimall.trade.task.application.*;
import org.springframework.stereotype.Component;

@Component
public class EnsureRefundHandler implements TaskHandler {
  private final RefundService refunds;
  private final Json json;

  public EnsureRefundHandler(RefundService refunds, Json json) {
    this.refunds = refunds;
    this.json = json;
  }

  public String type() {
    return "ENSURE_REFUND";
  }

  public TaskResult execute(TaskLease lease) {
    var c = json.read(lease.payload(), EnsureRefundCommand.class);
    refunds.ensure(c.businessKey(), c.orderNo(), c.afterSaleId(), c.reason());
    return TaskResult.done();
  }
}
