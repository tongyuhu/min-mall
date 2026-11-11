package com.minimall.trade.refund.application;

import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.DbRow;
import com.minimall.common.util.*;
import com.minimall.trade.aftersale.persistence.AfterSaleRepository;
import com.minimall.trade.order.persistence.OrderRepository;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.persistence.PaymentRepository;
import com.minimall.trade.refund.persistence.RefundRepository;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
  private final RefundRepository refunds;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final AfterSaleRepository aftersales;
  private final InventoryService stock;
  private final IntegrationTaskService tasks;
  private final Json json;
  private final Clock clock;

  public RefundService(
      RefundRepository refunds,
      OrderRepository orders,
      PaymentRepository payments,
      AfterSaleRepository aftersales,
      InventoryService stock,
      IntegrationTaskService tasks,
      Json json,
      Clock clock) {
    this.refunds = refunds;
    this.orders = orders;
    this.payments = payments;
    this.aftersales = aftersales;
    this.stock = stock;
    this.tasks = tasks;
    this.json = json;
    this.clock = clock;
  }

  @Transactional
  public String ensure(String key, String orderNo, Long afterId, String reason) {
    Checks.text(reason, 1, 255, "退款原因");
    var order = orders.byNo(orderNo, true);
    var previous = refunds.byKey(key);
    if (previous.isPresent()) {
      var r = previous.get();
      Checks.state(
          r.text("order_no").equals(orderNo)
              && Objects.equals(r.optionalNumber("after_sale_id"), afterId),
          "退款业务键内容冲突");
      return r.text("refund_no");
    }
    String paymentNo = null;
    var payment = payments.byOrder(orderNo);
    if (order.number("payable_fen") > 0) {
      var p = payment.orElseThrow(() -> BusinessException.conflict("缺少成功支付记录"));
      Checks.state("SUCCEEDED".equals(p.text("status")), "订单未成功收款");
      paymentNo = p.text("payment_no");
      payments.byNo(paymentNo, true);
    }
    DbRow after = null;
    if (afterId == null) {
      Checks.state(
          "CLOSED".equals(order.text("status"))
              && paymentNo != null
              && key.equals("LATE_PAYMENT:" + paymentNo),
          "仅关闭订单异常收款允许自动退款");
    } else {
      after = aftersales.byId(afterId);
      after = aftersales.byNo(after.text("after_sale_no"), true);
      Checks.state(
          after.text("order_no").equals(orderNo)
              && "REFUNDING".equals(after.text("status"))
              && key.equals("AFTER_SALE:" + after.text("after_sale_no")),
          "售后状态不支持退款");
      Checks.state(
          "REFUND_ONLY".equals(after.text("type")) || after.flag("return_received"), "退货尚未验收");
      Checks.state(Set.of("PAID", "SHIPPED", "COMPLETED").contains(order.text("status")), "订单不能退款");
    }
    String no = BusinessNumbers.next("R");
    refunds.insert(no, key, orderNo, paymentNo, afterId, order.number("payable_fen"), reason);
    if (after != null) aftersales.refund(after.text("after_sale_no"), no);
    if (order.number("payable_fen") == 0)
      applyResult(no, new RefundResult(no, RefundState.SUCCEEDED, clock.instant()));
    else tasks.enqueue("EXECUTE_REFUND", no, json.write(new RefundTaskCommand(no)));
    return no;
  }

  public RefundRequest request(String no) {
    var r = refunds.byNo(no, false);
    return new RefundRequest(no, r.text("payment_no"), r.number("amount_fen"));
  }

  @Transactional
  public void scheduleQuery(String no) {
    refunds.byNo(no, false);
    tasks.enqueue("QUERY_REFUND", no, json.write(new RefundTaskCommand(no)));
  }

  @Transactional
  public void applyResult(String no, RefundResult result) {
    if (result == null || !no.equals(result.refundNo()) || result.status() == null)
      throw BusinessException.bad("退款结果不匹配");
    var initial = refunds.byNo(no, false);
    var order = orders.byNo(initial.text("order_no"), true);
    DbRow after = null;
    if (initial.optionalNumber("after_sale_id") != null) {
      var r = aftersales.byId(initial.number("after_sale_id"));
      after = aftersales.byNo(r.text("after_sale_no"), true);
    }
    var refund = refunds.byNo(no, true);
    if ("SUCCEEDED".equals(refund.text("status"))) return;
    if (result.status() != RefundState.SUCCEEDED) {
      refunds.result(
          no,
          result.status().name(),
          null,
          result.status() == RefundState.FAILED
              ? "渠道退款失败，可重试"
              : result.status() == RefundState.UNKNOWN ? "渠道结果未知，等待查单" : null);
      return;
    }
    if (result.refundedAt() == null) throw BusinessException.conflict("成功退款缺少渠道时间");
    if (after != null) {
      Checks.state("REFUNDING".equals(after.text("status")), "售后状态与退款冲突");
      Checks.state(
          "REFUND_ONLY".equals(after.text("type")) || after.flag("return_received"), "退货未验收");
      stock.restock(
          no,
          orders.items(order.text("order_no")).stream()
              .map(r -> new StockLine(r.number("sku_id"), r.integer("quantity")))
              .toList());
      orders.transition(
          order.text("order_no"), order.text("status"), "REFUNDED", result.refundedAt(), "整单退款完成");
      aftersales.status(after.text("after_sale_no"), "COMPLETED");
      aftersales.log(after.text("after_sale_no"), "REFUNDED", "SYSTEM", null, "退款到账且库存归还");
      orders.activeAfterSale(order.text("order_no"), null);
    } else Checks.state("CLOSED".equals(order.text("status")), "异常收款退款不能改变履约订单");
    refunds.result(no, "SUCCEEDED", result.refundedAt(), null);
  }

  public RefundView get(long member, String no) {
    var r = refunds.byNo(no, false);
    orders.owned(member, r.text("order_no"));
    return view(r);
  }

  public List<RefundView> byOrder(long member, String order) {
    orders.owned(member, order);
    return refunds.byOrder(order).stream().map(this::view).toList();
  }

  public RefundView internal(String no) {
    return view(refunds.byNo(no, false));
  }

  private RefundView view(DbRow r) {
    return new RefundView(
        r.text("refund_no"),
        r.text("order_no"),
        r.number("amount_fen"),
        r.text("status"),
        r.instant("refunded_at"),
        r.text("last_error"));
  }
}
