package com.minimall.catalog.stock.persistence;

import com.minimall.common.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {
  private final Database db;

  public StockRepository(Database db) {
    this.db = db;
  }

  public void initialize(long sku) {
    db.update("INSERT IGNORE INTO sku_stock(sku_id,available,reserved) VALUES(?,0,0)", sku);
  }

  public DbRow get(long sku, boolean lock) {
    return db.required("SELECT * FROM sku_stock WHERE sku_id=?" + (lock ? " FOR UPDATE" : ""), sku);
  }

  public Optional<DbRow> log(String type, String no, long sku) {
    return db.first(
        "SELECT * FROM stock_log WHERE business_type=? AND business_no=? AND sku_id=?",
        type,
        no,
        sku);
  }

  public void change(long sku, int available, int reserved) {
    db.update(
        "UPDATE sku_stock SET available=?,reserved=?,updated_at=CURRENT_TIMESTAMP(6) WHERE"
            + " sku_id=?",
        available,
        reserved,
        sku);
  }

  public void log(
      String type,
      String no,
      long sku,
      int quantity,
      int beforeAvailable,
      int beforeReserved,
      int afterAvailable,
      int afterReserved) {
    db.insert(
        "INSERT INTO"
            + " stock_log(business_type,business_no,sku_id,quantity,before_available,before_reserved,after_available,after_reserved)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        type,
        no,
        sku,
        quantity,
        beforeAvailable,
        beforeReserved,
        afterAvailable,
        afterReserved);
  }
}
