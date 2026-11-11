package com.minimall.catalog.product.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.Database;
import com.minimall.common.util.Checks;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxonomyService {
  private final Database db;

  public TaxonomyService(Database db) {
    this.db = db;
  }

  private String table(String kind) {
    return switch (kind) {
      case "categories" -> "category";
      case "brands" -> "brand";
      case "specifications" -> "specification";
      case "specification-values" -> "specification_value";
      default -> throw BusinessException.bad("字典类型不存在");
    };
  }

  public record Entry(String id, String parentId, String name) {}

  public List<Entry> list(String kind) {
    String table = table(kind);
    return db.rows("SELECT * FROM " + table + " WHERE enabled=1 ORDER BY id").stream()
        .map(
            r -> {
              Long p =
                  r.optionalNumber(
                      table.equals("specification_value") ? "specification_id" : "parent_id");
              return new Entry(
                  Long.toString(r.number("id")), p == null ? null : p.toString(), r.text("name"));
            })
        .toList();
  }

  @Transactional
  public long save(String kind, Long id, Long parentId, String name) {
    String table = table(kind);
    name = Checks.text(name, 1, 64, "名称");
    if (table.equals("category")) {
      db.rows("SELECT id FROM category ORDER BY id FOR UPDATE");
      Long next = parentId;
      var seen = new HashSet<Long>();
      while (next != null) {
        if (Objects.equals(next, id) || !seen.add(next)) throw BusinessException.conflict("分类不能成环");
        next =
            db.required("SELECT parent_id FROM category WHERE id=? AND enabled=1", next)
                .optionalNumber("parent_id");
      }
    } else if (table.equals("specification_value")) {
      if (parentId == null) throw BusinessException.bad("规格值必须指定规格");
      db.required("SELECT id FROM specification WHERE id=? AND enabled=1", parentId);
    }
    String parentColumn =
        table.equals("category")
            ? "parent_id"
            : table.equals("specification_value") ? "specification_id" : null;
    if (id == null)
      return parentColumn == null
          ? db.insert("INSERT INTO " + table + "(name,enabled) VALUES(?,1)", name)
          : db.insert(
              "INSERT INTO " + table + "(name," + parentColumn + ",enabled) VALUES(?,?,1)",
              name,
              parentId);
    db.required("SELECT id FROM " + table + " WHERE id=? FOR UPDATE", id);
    if (parentColumn == null) db.update("UPDATE " + table + " SET name=? WHERE id=?", name, id);
    else
      db.update(
          "UPDATE " + table + " SET name=?," + parentColumn + "=? WHERE id=?", name, parentId, id);
    return id;
  }

  @Transactional
  public void archive(String kind, long id) {
    String table = table(kind);
    db.required("SELECT id FROM " + table + " WHERE id=? FOR UPDATE", id);
    long referenced =
        switch (kind) {
          case "categories" ->
              db.count("SELECT COUNT(*) AS value FROM product_spu WHERE category_id=?", id)
                  + db.count(
                      "SELECT COUNT(*) AS value FROM category WHERE parent_id=? AND enabled=1", id);
          case "brands" ->
              db.count("SELECT COUNT(*) AS value FROM product_spu WHERE brand_id=?", id);
          case "specifications" ->
              db.count(
                  "SELECT COUNT(*) AS value FROM specification_value WHERE specification_id=? AND"
                      + " enabled=1",
                  id);
          default -> db.count("SELECT COUNT(*) AS value FROM sku_spec_value WHERE value_id=?", id);
        };
    if (referenced > 0) throw BusinessException.conflict("仍有引用，不能归档");
    db.update("UPDATE " + table + " SET enabled=0 WHERE id=?", id);
  }
}
