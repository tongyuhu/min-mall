package com.minimall.trade.payment.mock;

import com.minimall.common.persistence.*;
import com.minimall.common.util.BusinessNumbers;
import com.minimall.trade.payment.gateway.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MockChannelRepository {
  private final Database db;

  public MockChannelRepository(Database db) {
    this.db = db;
  }

  public void create(PaymentRequest r) {
    db.update(
        "INSERT INTO"
            + " mock_payment_transaction(payment_no,order_no,amount_fen,currency,status,channel_transaction_no,expires_at)"
            + " VALUES(?,?,?,?,'PENDING',?,?) ON DUPLICATE KEY UPDATE payment_no=payment_no",
        r.paymentNo(),
        r.orderNo(),
        r.amountFen(),
        r.currency(),
        BusinessNumbers.next("MP"),
        r.expiresAt());
  }

  public Optional<DbRow> payment(String no, boolean lock) {
    return db.first(
        "SELECT * FROM mock_payment_transaction WHERE payment_no=?" + (lock ? " FOR UPDATE" : ""),
        no);
  }

  public void tombstone(String no) {
    db.update(
        "INSERT IGNORE INTO"
            + " mock_payment_transaction(payment_no,status,channel_transaction_no,currency,amount_fen)"
            + " VALUES(?,'CLOSED',?,'CNY',0)",
        no,
        BusinessNumbers.next("MP"));
  }

  public void close(String no) {
    db.update(
        "UPDATE mock_payment_transaction SET status='CLOSED' WHERE payment_no=? AND"
            + " status<>'SUCCEEDED'",
        no);
  }

  public void succeed(String no, Instant paidAt) {
    db.update(
        "UPDATE mock_payment_transaction SET status='SUCCEEDED',paid_at=? WHERE payment_no=?",
        paidAt,
        no);
  }

  public void fail(String no) {
    db.update(
        "UPDATE mock_payment_transaction SET status='FAILED' WHERE payment_no=? AND status IN"
            + " ('PENDING','FAILED')",
        no);
  }

  public void refundMode(String no, String mode) {
    db.update("UPDATE mock_payment_transaction SET refund_mode=? WHERE payment_no=?", mode, no);
  }

  public Optional<DbRow> refund(String no) {
    return db.first("SELECT * FROM mock_refund_transaction WHERE refund_no=?", no);
  }

  public Optional<DbRow> refundByPayment(String no) {
    return db.first("SELECT * FROM mock_refund_transaction WHERE payment_no=?", no);
  }

  public void createRefund(RefundRequest r, String state, Instant at) {
    db.update(
        "INSERT INTO mock_refund_transaction(refund_no,payment_no,amount_fen,status,refunded_at)"
            + " VALUES(?,?,?,?,?)",
        r.refundNo(),
        r.paymentNo(),
        r.amountFen(),
        state,
        at);
  }

  public void refundState(String no, String state, Instant at) {
    db.update(
        "UPDATE mock_refund_transaction SET status=?,refunded_at=? WHERE refund_no=? AND"
            + " status<>'SUCCEEDED'",
        state,
        at,
        no);
  }
}
