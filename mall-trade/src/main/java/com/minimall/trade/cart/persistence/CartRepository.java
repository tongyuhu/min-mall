package com.minimall.trade.cart.persistence;

import com.minimall.common.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {
  private final Database db;

  public CartRepository(Database db) {
    this.db = db;
  }

  public void lock(long member) {
    db.update("INSERT IGNORE INTO cart_owner(member_id) VALUES(?)", member);
    db.required("SELECT member_id FROM cart_owner WHERE member_id=? FOR UPDATE", member);
  }

  public List<DbRow> list(long member) {
    return db.rows("SELECT * FROM cart_item WHERE member_id=? ORDER BY id", member);
  }

  public Optional<DbRow> bySku(long member, long sku) {
    return db.first("SELECT * FROM cart_item WHERE member_id=? AND sku_id=?", member, sku);
  }

  public DbRow owned(long member, long id) {
    return db.required("SELECT * FROM cart_item WHERE member_id=? AND id=?", member, id);
  }

  public void insert(long member, long sku, int quantity) {
    db.insert(
        "INSERT INTO cart_item(member_id,sku_id,quantity,selected) VALUES(?,?,?,1)",
        member,
        sku,
        quantity);
  }

  public void update(long id, int quantity, boolean selected) {
    db.update("UPDATE cart_item SET quantity=?,selected=? WHERE id=?", quantity, selected, id);
  }

  public void remove(long id) {
    db.update("DELETE FROM cart_item WHERE id=?", id);
  }
}
