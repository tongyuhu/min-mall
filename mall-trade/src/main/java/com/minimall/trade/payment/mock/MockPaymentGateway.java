package com.minimall.trade.payment.mock;

import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.DbRow;
import com.minimall.common.util.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("(local | test) & !prod")
@ConditionalOnProperty(name = "mall.simulation.enabled", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {
  private final MockChannelRepository repository;
  private final IntegrationTaskService tasks;
  private final Json json;
  private final Clock clock;
  private final String key;
  private final String merchant;

  public MockPaymentGateway(
      MockChannelRepository repository,
      IntegrationTaskService tasks,
      Json json,
      Clock clock,
      @Value("${mall.simulation.key}") String key,
      @Value("${mall.simulation.merchant-id}") String merchant) {
    this.repository = repository;
    this.tasks = tasks;
    this.json = json;
    this.clock = clock;
    this.key = key;
    this.merchant = merchant;
  }

  @Transactional
  public PaymentResult create(PaymentRequest r) {
    if (r == null || r.amountFen() <= 0 || !"CNY".equals(r.currency()) || r.expiresAt() == null)
      throw BusinessException.bad("渠道支付参数不合法");
    repository.create(r);
    var row = repository.payment(r.paymentNo(), true).orElseThrow();
    if (row.text("order_no") != null
        && (!row.text("order_no").equals(r.orderNo())
            || row.number("amount_fen") != r.amountFen()
            || !row.text("currency").equals(r.currency())))
      throw BusinessException.conflict("支付业务号载荷不一致");
    return result(row);
  }

  public PaymentResult query(String no) {
    return repository
        .payment(no, false)
        .map(this::result)
        .orElse(new PaymentResult(no, null, PaymentState.UNKNOWN, null, 0, "CNY", Map.of()));
  }

  @Transactional
  public PaymentResult close(String no) {
    repository.tombstone(no);
    var row = repository.payment(no, true).orElseThrow();
    if (!"SUCCEEDED".equals(row.text("status"))) repository.close(no);
    return result(repository.payment(no, false).orElseThrow());
  }

  private PaymentResult result(DbRow row) {
    return new PaymentResult(
        row.text("payment_no"),
        row.text("channel_transaction_no"),
        PaymentState.valueOf(row.text("status")),
        row.instant("paid_at"),
        row.number("amount_fen"),
        row.text("currency"),
        Map.of("mode", "LOCAL_SIMULATION", "paymentNo", row.text("payment_no")));
  }

  @Transactional
  public PaymentResult succeed(
      String no,
      Instant suppliedTime,
      boolean anomaly,
      int notifications,
      int delaySeconds,
      int failures) {
    if (notifications < 1
        || notifications > 10
        || delaySeconds < 0
        || delaySeconds > 3600
        || failures < 0
        || failures > 6) throw BusinessException.bad("模拟通知参数不合法");
    var row = repository.payment(no, true).orElseThrow(BusinessException::missing);
    if (!anomaly) {
      Checks.state(
          "PENDING".equals(row.text("status")) || "SUCCEEDED".equals(row.text("status")),
          "渠道单已经关闭");
      if (!"SUCCEEDED".equals(row.text("status"))
          && !clock.instant().isBefore(row.instant("expires_at")))
        throw BusinessException.conflict("支付已过期，请使用异常注入接口演示补偿");
    }
    Instant paidAt =
        "SUCCEEDED".equals(row.text("status"))
            ? row.instant("paid_at")
            : (anomaly && suppliedTime != null ? suppliedTime : clock.instant())
                .truncatedTo(ChronoUnit.MICROS);
    repository.succeed(no, paidAt);
    row = repository.payment(no, false).orElseThrow();
    var payment =
        new VerifiedPayment(
            BusinessNumbers.next("N"),
            merchant,
            no,
            row.text("channel_transaction_no"),
            row.number("amount_fen"),
            row.text("currency"),
            paidAt,
            PaymentState.SUCCEEDED);
    for (int i = 0; i < notifications; i++) {
      var envelope =
          new NotificationEnvelope(payment, clock.instant().plusSeconds(delaySeconds), failures);
      tasks.enqueue("NOTIFY_PAYMENT", payment.notificationId() + ":" + i, json.write(envelope));
    }
    return result(row);
  }

  public VerifiedPayment verify(byte[] rawBody, Map<String, String> headers) {
    String supplied =
        headers.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase("X-Signature"))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    if (rawBody == null || rawBody.length > 65536 || !Digests.equal(sign(rawBody), supplied))
      throw new BusinessException("BAD_SIGNATURE", 400, "渠道签名无效");
    VerifiedPayment value;
    try {
      value = json.read(new String(rawBody, StandardCharsets.UTF_8), VerifiedPayment.class);
    } catch (RuntimeException e) {
      throw BusinessException.bad("通知格式不合法");
    }
    if (!merchant.equals(value.merchantId())
        || !"CNY".equals(value.currency())
        || value.amountFen() < 0
        || value.paymentNo() == null
        || value.channelTransactionNo() == null
        || value.notificationId() == null
        || value.status() == null) throw BusinessException.bad("渠道通知参数不合法");
    return value;
  }

  public String sign(byte[] body) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(body));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public RefundResult refund(RefundRequest request) {
    var payment =
        repository.payment(request.paymentNo(), true).orElseThrow(BusinessException::missing);
    Checks.state(
        "SUCCEEDED".equals(payment.text("status"))
            && request.amountFen() == payment.number("amount_fen"),
        "退款金额或支付状态不正确");
    var existing = repository.refundByPayment(request.paymentNo());
    if (existing.isPresent() && !existing.get().text("refund_no").equals(request.refundNo()))
      throw BusinessException.conflict("该支付已经存在退款单");
    String mode = payment.text("refund_mode");
    String state =
        mode.equals("SUCCESS") ? "SUCCEEDED" : mode.equals("FAIL") ? "FAILED" : "PENDING";
    Instant at = state.equals("SUCCEEDED") ? clock.instant() : null;
    if (existing.isEmpty()) repository.createRefund(request, state, at);
    else if ("FAILED".equals(existing.get().text("status")))
      repository.refundState(request.refundNo(), state, at);
    var result = queryRefund(request.refundNo());
    return mode.equals("UNKNOWN")
        ? new RefundResult(request.refundNo(), RefundState.UNKNOWN, null)
        : result;
  }

  public RefundResult queryRefund(String no) {
    return repository
        .refund(no)
        .map(
            r ->
                new RefundResult(
                    no, RefundState.valueOf(r.text("status")), r.instant("refunded_at")))
        .orElse(new RefundResult(no, RefundState.UNKNOWN, null));
  }

  @Transactional
  public void refundMode(String no, String mode) {
    if (!Set.of("SUCCESS", "PENDING", "FAIL", "UNKNOWN").contains(mode))
      throw BusinessException.bad("退款模式无效");
    repository.payment(no, true).orElseThrow(BusinessException::missing);
    repository.refundMode(no, mode);
  }

  @Transactional
  public void resolveRefund(String no, boolean success) {
    var row = repository.refund(no).orElseThrow(BusinessException::missing);
    repository.payment(row.text("payment_no"), true).orElseThrow();
    row = repository.refund(no).orElseThrow();
    if ("SUCCEEDED".equals(row.text("status"))) {
      Checks.state(success, "成功退款不可改为失败");
      return;
    }
    repository.refundState(no, success ? "SUCCEEDED" : "FAILED", success ? clock.instant() : null);
  }
}
