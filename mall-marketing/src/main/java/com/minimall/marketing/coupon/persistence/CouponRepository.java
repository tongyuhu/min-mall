package com.minimall.marketing.coupon.persistence;

import com.minimall.common.persistence.*;
import com.minimall.marketing.coupon.application.CouponTemplateInput;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CouponRepository {
  private final Database db;

  public CouponRepository(Database db) {
    this.db = db;
  }

  public DbRow template(long id, boolean lock) {
    return db.required(
        "SELECT * FROM coupon_template WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public long insertTemplate(CouponTemplateInput c) {
    return db.insert(
        "INSERT INTO"
            + " coupon_template(name,threshold_fen,discount_fen,total,issued,valid_from,valid_until,enabled)"
            + " VALUES(?,?,?,?,0,?,?,1)",
        c.name(),
        c.thresholdFen(),
        c.discountFen(),
        c.total(),
        c.validFrom(),
        c.validUntil());
  }

  public void updateTemplate(long id, CouponTemplateInput c) {
    db.update(
        "UPDATE coupon_template SET"
            + " name=?,threshold_fen=?,discount_fen=?,total=?,valid_from=?,valid_until=? WHERE"
            + " id=?",
        c.name(),
        c.thresholdFen(),
        c.discountFen(),
        c.total(),
        c.validFrom(),
        c.validUntil(),
        id);
  }

  public void enabled(long id, boolean value) {
    db.update("UPDATE coupon_template SET enabled=? WHERE id=?", value, id);
  }

  public boolean increment(long id) {
    return db.update("UPDATE coupon_template SET issued=issued+1 WHERE id=? AND issued<total", id)
        == 1;
  }

  public long issue(long member, DbRow t) {
    return db.insert(
        "INSERT INTO"
            + " member_coupon(member_id,template_id,name,threshold_fen,discount_fen,valid_from,valid_until,status)"
            + " VALUES(?,?,?,?,?,?,?,'AVAILABLE')",
        member,
        t.number("id"),
        t.text("name"),
        t.number("threshold_fen"),
        t.number("discount_fen"),
        t.instant("valid_from"),
        t.instant("valid_until"));
  }

  public DbRow coupon(long member, long id, boolean lock) {
    return db.required(
        "SELECT * FROM member_coupon WHERE id=? AND member_id=?" + (lock ? " FOR UPDATE" : ""),
        id,
        member);
  }

  public Optional<DbRow> byOrder(String no) {
    return db.first("SELECT * FROM member_coupon WHERE order_no=? FOR UPDATE", no);
  }

  public void reserve(long id, String no) {
    db.update("UPDATE member_coupon SET status='RESERVED',order_no=? WHERE id=?", no, id);
  }

  public void status(long id, String status, boolean clear) {
    db.update(
        "UPDATE member_coupon SET status=?" + (clear ? ",order_no=NULL" : "") + " WHERE id=?",
        status,
        id);
  }

  public void log(long id, String no, String action) {
    db.insert("INSERT INTO coupon_log(coupon_id,order_no,action) VALUES(?,?,?)", id, no, action);
  }

  public List<DbRow> mine(long member) {
    return db.rows(
        "SELECT * FROM member_coupon WHERE member_id=? ORDER BY id DESC LIMIT 200", member);
  }

  public List<DbRow> templates() {
    return templates(false);
  }

  public List<DbRow> templates(boolean all) {
    return db.rows(
        "SELECT * FROM coupon_template "
            + (all ? "" : "WHERE enabled=1 ")
            + "ORDER BY id DESC LIMIT 200");
  }
}
