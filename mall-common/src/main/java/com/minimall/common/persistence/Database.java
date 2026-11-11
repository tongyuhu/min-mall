package com.minimall.common.persistence;

import com.minimall.common.api.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Database {
  private final SqlMapper mapper;

  public Database(SqlMapper mapper) {
    this.mapper = mapper;
  }

  private Map<String, Object> parameters(String sql, Object... args) {
    var result = new HashMap<String, Object>();
    result.put("sql", sql);
    result.put(
        "args",
        // DATETIME 没有时区；显式写 UTC 墙上时间，避免驱动使用 JVM 默认时区。
        // 数据库精度为微秒，先截断而不是让 MySQL 四舍五入，保证事件重放一致。
        Arrays.stream(args)
            .map(
                v ->
                    v instanceof Instant i
                        ? LocalDateTime.ofInstant(i.truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC)
                        : v)
            .toArray());
    return result;
  }

  public List<DbRow> rows(String sql, Object... args) {
    return mapper.select(parameters(sql, args)).stream().map(DbRow::new).toList();
  }

  public Optional<DbRow> first(String sql, Object... args) {
    return rows(sql, args).stream().findFirst();
  }

  public DbRow required(String sql, Object... args) {
    return first(sql, args).orElseThrow(BusinessException::missing);
  }

  public int update(String sql, Object... args) {
    return mapper.update(parameters(sql, args));
  }

  public long insert(String sql, Object... args) {
    var p = parameters(sql, args);
    mapper.insert(p);
    return ((Number) p.get("generatedId")).longValue();
  }

  public long count(String sql, Object... args) {
    var row = required(sql, args);
    return row.number("value");
  }
}
