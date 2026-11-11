package com.minimall.trade.refund.task;
import com.minimall.trade.refund.application.*;import com.minimall.trade.payment.gateway.*;import com.minimall.trade.task.application.*;import com.minimall.common.util.Json;import org.springframework.stereotype.Component;
@Component public class ExecuteRefundHandler implements TaskHandler {
 private final RefundService refunds;private final PaymentGateway gateway;private final Json json;public ExecuteRefundHandler(RefundService refunds,PaymentGateway gateway,Json json){this.refunds=refunds;this.gateway=gateway;this.json=json;}
 public String type(){return "EXECUTE_REFUND";}public TaskResult execute(TaskLease lease){String no=json.read(lease.payload(),RefundTaskCommand.class).refundNo();if("SUCCEEDED".equals(refunds.internal(no).status()))return TaskResult.done();var result=gateway.queryRefund(no);if(result.status()==RefundState.UNKNOWN||result.status()==RefundState.FAILED)result=gateway.refund(refunds.request(no));refunds.applyResult(no,result);if(result.status()==RefundState.FAILED)return TaskResult.retry("渠道退款失败");if(result.status()!=RefundState.SUCCEEDED)refunds.scheduleQuery(no);return TaskResult.done();}
}
