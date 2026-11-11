package com.minimall.trade.order.persistence;

import com.minimall.common.api.*;
import com.minimall.common.persistence.*;
import com.minimall.common.util.Json;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.user.address.application.AddressSnapshot;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
  private final Database db;
  private final Json json;

  public OrderRepository(Database db, Json json) {
    this.db = db;
    this.json = json;
  }

  public Optional<DbRow> byKey(long member, String key) {
    return db.first(
        "SELECT * FROM trade_order WHERE member_id=? AND idempotency_key=?", member, key);
  }

  public DbRow byNo(String no, boolean lock) {
    return db.required(
        "SELECT * FROM trade_order WHERE order_no=?" + (lock ? " FOR UPDATE" : ""), no);
  }

  public DbRow owned(long member, String no) {
    return db.required("SELECT * FROM trade_order WHERE member_id=? AND order_no=?", member, no);
  }

  public void insert(long member, String no, String key, String hash, Instant now) {
    db.insert(
        "INSERT INTO"
            + " trade_order(order_no,member_id,idempotency_key,request_hash,status,created_at,expires_at)"
            + " VALUES(?,?,?,?,'PENDING_PAYMENT',?,?)",
        no,
        member,
        key,
        hash,
        now,
        now.plusSeconds(1800));
  }

  public void fill(String no, CheckoutCommand command, CheckoutQuote q) {
    db.update(
        "UPDATE trade_order SET"
            + " goods_fen=?,discount_fen=?,freight_fen=?,payable_fen=?,coupon_id=?,zero_settled=?"
            + " WHERE order_no=?",
        q.goodsFen(),
        q.discountFen(),
        q.freightFen(),
        q.payableFen(),
        command.couponId(),
        q.payableFen() == 0,
        no);
    db.insert(
        "INSERT INTO order_address(order_no,snapshot_json) VALUES(?,?)",
        no,
        json.write(q.address()));
    for (int i = 0; i < q.skus().size(); i++) {
      var sku = q.skus().get(i);
      db.insert(
          "INSERT INTO"
              + " order_item(order_no,sku_id,title,specs,image_url,quantity,unit_price_fen,payable_fen)"
              + " VALUES(?,?,?,?,?,?,?,?)",
          no,
          sku.skuId(),
          sku.title(),
          sku.specs(),
          sku.imageUrl(),
          command.lines().get(i).quantity(),
          sku.priceFen(),
          q.linePayableFen().get(i));
    }
    log(no, null, "PENDING_PAYMENT", "创建订单");
  }

  public List<DbRow> items(String no) {
    return db.rows("SELECT * FROM order_item WHERE order_no=? ORDER BY sku_id", no);
  }

  public void transition(String no, String expected, String target, Instant at, String reason) {
    String date =
        switch (target) {
          case "PAID" -> ",paid_at=?";
          case "COMPLETED" -> ",completed_at=?";
          default -> "";
        };
    int count =
        date.isEmpty()
            ? db.update(
                "UPDATE trade_order SET status=?,updated_at=CURRENT_TIMESTAMP(6) WHERE order_no=?"
                    + " AND status=?",
                target,
                no,
                expected)
            : db.update(
                "UPDATE trade_order SET status=?"
                    + date
                    + ",updated_at=CURRENT_TIMESTAMP(6) WHERE order_no=? AND status=?",
                target,
                at,
                no,
                expected);
    if (count != 1) throw BusinessException.conflict("订单状态已变化");
    log(no, expected, target, reason);
  }

  public void closeRequested(String no) {
    db.update("UPDATE trade_order SET close_requested=1 WHERE order_no=?", no);
  }

  public void activeAfterSale(String no, String afterSaleNo) {
    db.update("UPDATE trade_order SET active_after_sale_no=? WHERE order_no=?", afterSaleNo, no);
  }

  public void log(String no, String from, String to, String reason) {
    db.insert(
        "INSERT INTO order_status_log(order_no,from_status,to_status,reason) VALUES(?,?,?,?)",
        no,
        from,
        to,
        reason);
  }

  public OrderView view(DbRow r) {
    String no = r.text("order_no");
    return new OrderView(
        no,
        r.text("status"),
        r.number("goods_fen"),
        r.number("discount_fen"),
        r.number("freight_fen"),
        r.number("payable_fen"),
        items(no).stream()
            .map(
                i ->
                    new OrderItemView(
                        Long.toString(i.number("sku_id")),
                        i.text("title"),
                        i.text("specs"),
                        i.text("image_url"),
                        i.integer("quantity"),
                        i.number("unit_price_fen"),
                        i.number("payable_fen")))
            .toList(),
        json.read(
            db.required("SELECT snapshot_json FROM order_address WHERE order_no=?", no)
                .text("snapshot_json"),
            AddressSnapshot.class),
        r.instant("created_at"),
        r.instant("expires_at"),
        r.instant("paid_at"),
        r.instant("completed_at"),
        r.text("active_after_sale_no"),
        r.flag("close_requested"));
  }

  public List<DbRow> list(long member, PageQuery page) {
    return db.rows(
        "SELECT * FROM trade_order WHERE member_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
        member,
        page.size(),
        page.offset());
  }

  public long count(long member) {
    return db.count("SELECT COUNT(*) AS value FROM trade_order WHERE member_id=?", member);
  }

  public List<DbRow> expired(Instant now, int limit) {
    return db.rows(
        "SELECT * FROM trade_order WHERE status='PENDING_PAYMENT' AND expires_at<=? ORDER BY"
            + " expires_at,id LIMIT ?",
        now,
        limit);
  }
}
