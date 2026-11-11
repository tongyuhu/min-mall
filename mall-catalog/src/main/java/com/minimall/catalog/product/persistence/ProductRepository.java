package com.minimall.catalog.product.persistence;

import com.minimall.catalog.product.application.*;
import com.minimall.common.api.*;
import com.minimall.common.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
  private final Database db;

  public ProductRepository(Database db) {
    this.db = db;
  }

  public DbRow spu(long id, boolean lock) {
    return db.required("SELECT * FROM product_spu WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public List<DbRow> skus(long id) {
    return db.rows("SELECT * FROM product_sku WHERE spu_id=? ORDER BY id", id);
  }

  public DbRow sku(long id) {
    return db.required("SELECT * FROM product_sku WHERE id=?", id);
  }

  public List<String> images(long id) {
    return db.rows("SELECT url FROM product_image WHERE spu_id=? ORDER BY position,id", id).stream()
        .map(r -> r.text("url"))
        .toList();
  }

  public long createSpu(ProductCommand c) {
    return db.insert(
        "INSERT INTO product_spu(category_id,brand_id,title,on_sale,sale_version)"
            + " VALUES(?,?,?,0,1)",
        c.categoryId(),
        c.brandId(),
        c.title());
  }

  public void updateSpu(long id, ProductCommand c) {
    db.update(
        "UPDATE product_spu SET"
            + " category_id=?,brand_id=?,title=?,sale_version=sale_version+1,updated_at=CURRENT_TIMESTAMP(6)"
            + " WHERE id=?",
        c.categoryId(),
        c.brandId(),
        c.title(),
        id);
  }

  public void onSale(long id, boolean value) {
    db.update(
        "UPDATE product_spu SET"
            + " on_sale=?,sale_version=sale_version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",
        value,
        id);
  }

  public void images(long id, List<String> urls) {
    db.update("DELETE FROM product_image WHERE spu_id=?", id);
    for (int i = 0; i < urls.size(); i++)
      db.insert("INSERT INTO product_image(spu_id,url,position) VALUES(?,?,?)", id, urls.get(i), i);
  }

  public long createSku(long id, String key, String specs, long price) {
    return db.insert(
        "INSERT INTO product_sku(spu_id,spec_key,specs,price_fen,enabled) VALUES(?,?,?,?,1)",
        id,
        key,
        specs,
        price);
  }

  public void updateSku(long sku, String specs, long price) {
    db.update("UPDATE product_sku SET specs=?,price_fen=?,enabled=1 WHERE id=?", specs, price, sku);
  }

  public void disableSku(long sku) {
    db.update("UPDATE product_sku SET enabled=0 WHERE id=?", sku);
  }

  public void linkValue(long sku, long value) {
    db.insert("INSERT INTO sku_spec_value(sku_id,value_id) VALUES(?,?)", sku, value);
  }

  public void priceLog(long sku, Long oldPrice, long price) {
    db.insert(
        "INSERT INTO product_price_log(sku_id,old_price_fen,new_price_fen) VALUES(?,?,?)",
        sku,
        oldPrice,
        price);
  }

  public DbRow specificationValue(long id) {
    return db.required(
        "SELECT v.*,s.name AS specification_name FROM specification_value v JOIN specification s ON"
            + " s.id=v.specification_id WHERE v.id=? AND v.enabled=1 AND s.enabled=1",
        id);
  }

  public void validateReferences(long category, Long brand) {
    db.required("SELECT id FROM category WHERE id=? AND enabled=1", category);
    if (brand != null) db.required("SELECT id FROM brand WHERE id=? AND enabled=1", brand);
  }

  public List<DbRow> search(ProductQuery q, PageQuery p) {
    return search(q, p, false);
  }

  public List<DbRow> search(ProductQuery q, PageQuery p, boolean includeHidden) {
    var args = new ArrayList<Object>();
    String where = where(q, args, includeHidden);
    args.add(p.size());
    args.add(p.offset());
    String sort =
        switch (p.sort()) {
          case "price" -> "min_price";
          case "createdAt" -> "p.created_at DESC";
          default -> "p.id DESC";
        };
    return db.rows(
        "SELECT p.*, (SELECT MIN(price_fen) FROM product_sku WHERE spu_id=p.id AND enabled=1) AS"
            + " min_price FROM product_spu p WHERE "
            + where
            + " ORDER BY "
            + sort
            + ",p.id LIMIT ? OFFSET ?",
        args.toArray());
  }

  public long count(ProductQuery q) {
    return count(q, false);
  }

  public long count(ProductQuery q, boolean includeHidden) {
    var args = new ArrayList<Object>();
    return db.count(
        "SELECT COUNT(*) AS value FROM product_spu p WHERE " + where(q, args, includeHidden),
        args.toArray());
  }

  private String where(ProductQuery q, List<Object> a, boolean includeHidden) {
    StringBuilder w = new StringBuilder(includeHidden ? "1=1" : "p.on_sale=1");
    if (q.categoryId() != null) {
      w.append(" AND p.category_id=?");
      a.add(q.categoryId());
    }
    if (q.brandId() != null) {
      w.append(" AND p.brand_id=?");
      a.add(q.brandId());
    }
    if (q.keyword() != null && !q.keyword().isBlank()) {
      w.append(" AND p.title LIKE ? ESCAPE '!'");
      a.add("%" + q.keyword().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    }
    if (q.minPriceFen() != null || q.maxPriceFen() != null) {
      w.append(" AND EXISTS(SELECT 1 FROM product_sku s WHERE s.spu_id=p.id AND s.enabled=1");
      if (q.minPriceFen() != null) {
        w.append(" AND s.price_fen>=?");
        a.add(q.minPriceFen());
      }
      if (q.maxPriceFen() != null) {
        w.append(" AND s.price_fen<=?");
        a.add(q.maxPriceFen());
      }
      w.append(")");
    }
    return w.toString();
  }

  public DbRow lockSku(long id) {
    return db.required("SELECT * FROM product_sku WHERE id=? FOR UPDATE", id);
  }
}
