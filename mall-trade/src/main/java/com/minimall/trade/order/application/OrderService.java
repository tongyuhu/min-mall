package com.minimall.trade.order.application;

import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.*;
import com.minimall.common.util.*;
import com.minimall.marketing.coupon.application.CouponService;
import com.minimall.trade.cart.application.CartService;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.persistence.OrderRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
  private final OrderRepository repository;
  private final CheckoutService checkout;
  private final InventoryService inventory;
  private final CouponService coupons;
  private final CartService cart;
  private final Clock clock;
  private final Json json;
  private final TransactionTemplate tx;

  public OrderService(
      OrderRepository repository,
      CheckoutService checkout,
      InventoryService inventory,
      CouponService coupons,
      CartService cart,
      Clock clock,
      Json json,
      PlatformTransactionManager manager) {
    this.repository = repository;
    this.checkout = checkout;
    this.inventory = inventory;
    this.coupons = coupons;
    this.cart = cart;
    this.clock = clock;
    this.json = json;
    this.tx = new TransactionTemplate(manager);
  }

  public OrderView place(long member, String key, PlaceOrderCommand command) {
    Checks.text(key, 1, 100, "幂等键");
    if (command == null || command.quoteFingerprint() == null)
      throw BusinessException.bad("缺少结算确认信息");
    var normalized = checkout.normalize(command.checkout());
    var input = new PlaceOrderCommand(normalized, command.quoteFingerprint(), command.fromCart());
    String hash = Digests.sha256(json.write(input));
    var existing = repository.byKey(member, key);
    if (existing.isPresent()) return replay(existing.get(), hash);
    try {
      return tx.execute(
          status -> {
            String no = BusinessNumbers.next("O");
            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            repository.insert(member, no, key, hash, now);
            var quote = checkout.lockQuote(member, normalized);
            if (!Digests.equal(quote.fingerprint(), input.quoteFingerprint()))
              throw BusinessException.conflict("结算信息已变化，请重新确认");
            var lines =
                normalized.lines().stream()
                    .map(l -> new StockLine(l.skuId(), l.quantity()))
                    .toList();
            inventory.reserve(no, lines);
            if (normalized.couponId() != null) {
              var reserved =
                  coupons.reserve(member, normalized.couponId(), no, quote.goodsFen(), now);
              Checks.state(reserved.discountFen() == quote.discountFen(), "优惠已变化");
            }
            repository.fill(no, normalized, quote);
            if (input.fromCart()) cart.consume(member, normalized.lines());
            if (quote.payableFen() == 0) {
              inventory.commitReservation(no, lines);
              coupons.use(no);
              repository.transition(no, "PENDING_PAYMENT", "PAID", now, "零元订单结算");
            }
            return repository.view(repository.byNo(no, false));
          });
    } catch (DuplicateKeyException e) {
      var original = repository.byKey(member, key);
      if (original.isEmpty()) throw e;
      return replay(original.get(), hash);
    }
  }

  private OrderView replay(com.minimall.common.persistence.DbRow row, String hash) {
    if (!Digests.equal(row.text("request_hash"), hash))
      throw BusinessException.conflict("同一幂等键不能提交不同内容");
    return repository.view(row);
  }

  public OrderView detail(long member, String no) {
    return repository.view(repository.owned(member, no));
  }

  public OrderView adminDetail(String no) {
    return repository.view(repository.byNo(no, false));
  }

  public PageResult<OrderView> list(long member, PageQuery page) {
    return new PageResult<>(
        repository.list(member, page).stream().map(repository::view).toList(),
        repository.count(member),
        page.page(),
        page.size());
  }
}
