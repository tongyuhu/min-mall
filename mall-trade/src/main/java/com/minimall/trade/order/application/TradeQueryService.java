package com.minimall.trade.order.application;

import com.minimall.common.api.*;
import com.minimall.common.persistence.Database;
import com.minimall.trade.order.persistence.OrderRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeQueryService {
  private final Database db;
  private final OrderRepository orders;

  public TradeQueryService(Database db, OrderRepository orders) {
    this.db = db;
    this.orders = orders;
  }

  public PageResult<OrderView> search(OrderFilter f, PageQuery page) {
    var args = new ArrayList<Object>();
    StringBuilder w = new StringBuilder("1=1");
    if (f.orderNo() != null) {
      w.append(" AND order_no=?");
      args.add(f.orderNo());
    }
    if (f.memberId() != null) {
      w.append(" AND member_id=?");
      args.add(f.memberId());
    }
    if (f.status() != null) {
      w.append(" AND status=?");
      args.add(f.status());
    }
    if (f.from() != null) {
      w.append(" AND created_at>=?");
      args.add(f.from());
    }
    if (f.until() != null) {
      w.append(" AND created_at<?");
      args.add(f.until());
    }
    long count = db.count("SELECT COUNT(*) AS value FROM trade_order WHERE " + w, args.toArray());
    args.add(page.size());
    args.add(page.offset());
    var items =
        db
            .rows(
                "SELECT * FROM trade_order WHERE " + w + " ORDER BY id DESC LIMIT ? OFFSET ?",
                args.toArray())
            .stream()
            .map(orders::view)
            .toList();
    return new PageResult<>(items, count, page.page(), page.size());
  }

  @Transactional(readOnly = true)
  public TradeStatistics statistics(Instant from, Instant until) {
    long created =
        db.count(
            "SELECT COUNT(*) AS value FROM trade_order WHERE created_at>=? AND created_at<?",
            from,
            until);
    long received =
        db.count(
            "SELECT COALESCE(SUM(amount_fen),0) AS value FROM payment_order WHERE"
                + " status='SUCCEEDED' AND paid_at>=? AND paid_at<?",
            from,
            until);
    long refunded =
        db.count(
            "SELECT COALESCE(SUM(amount_fen),0) AS value FROM refund_order WHERE status='SUCCEEDED'"
                + " AND refunded_at>=? AND refunded_at<?",
            from,
            until);
    return new TradeStatistics(
        created,
        received,
        refunded,
        Math.subtractExact(received, refunded),
        db.count(
            "SELECT COUNT(*) AS value FROM trade_order WHERE status='PAID' AND active_after_sale_no"
                + " IS NULL"),
        db.count("SELECT COUNT(*) AS value FROM after_sale WHERE status='WAIT_REVIEW'"),
        db.count("SELECT COUNT(*) AS value FROM integration_task WHERE status='MANUAL'"));
  }
}
