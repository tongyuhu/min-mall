package com.minimall.trade.checkout.application;

import com.minimall.catalog.product.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.common.util.*;
import com.minimall.marketing.coupon.application.*;
import com.minimall.trade.checkout.domain.PriceCalculator;
import com.minimall.user.address.application.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CheckoutService {
  private final CatalogFacade catalog;
  private final AddressService addresses;
  private final CouponService coupons;
  private final Clock clock;
  private final Json json;
  private final long freight;

  public CheckoutService(
      CatalogFacade catalog,
      AddressService addresses,
      CouponService coupons,
      Clock clock,
      Json json,
      @Value("${mall.freight-fen:0}") long freight) {
    if (freight < 0) throw new IllegalArgumentException("运费不能为负");
    this.catalog = catalog;
    this.addresses = addresses;
    this.coupons = coupons;
    this.clock = clock;
    this.json = json;
    this.freight = freight;
  }

  public CheckoutCommand normalize(CheckoutCommand c) {
    if (c == null
        || c.lines() == null
        || c.lines().isEmpty()
        || c.lines().size() > 200
        || c.addressId() <= 0) throw BusinessException.bad("结算参数不合法");
    var quantities = new TreeMap<Long, Integer>();
    for (var line : c.lines()) {
      if (line == null || line.skuId() <= 0) throw BusinessException.bad("SKU不合法");
      Checks.quantity(line.quantity());
      int count = quantities.getOrDefault(line.skuId(), 0) + line.quantity();
      quantities.put(line.skuId(), Checks.quantity(count));
    }
    if (quantities.size() > 50) throw BusinessException.bad("一次最多购买50种SKU");
    return new CheckoutCommand(
        c.addressId(),
        quantities.entrySet().stream()
            .map(e -> new CheckoutLine(e.getKey(), e.getValue()))
            .toList(),
        c.couponId());
  }

  public CheckoutQuote preview(long member, CheckoutCommand input) {
    return quote(member, normalize(input), false);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public CheckoutQuote lockQuote(long member, CheckoutCommand input) {
    return quote(member, normalize(input), true);
  }

  private CheckoutQuote quote(long member, CheckoutCommand c, boolean lock) {
    var ids = c.lines().stream().map(CheckoutLine::skuId).toList();
    var skus = lock ? catalog.lockSaleSkus(ids) : catalog.loadSaleSkus(ids);
    var address = addresses.snapshot(member, c.addressId());
    var lineAmounts = new ArrayList<Long>();
    long total = 0;
    try {
      for (int i = 0; i < skus.size(); i++) {
        long amount = Math.multiplyExact(skus.get(i).priceFen(), c.lines().get(i).quantity());
        lineAmounts.add(amount);
        total = Math.addExact(total, amount);
      }
    } catch (ArithmeticException e) {
      throw BusinessException.bad("金额超出范围");
    }
    Instant now = clock.instant();
    var coupon = coupons.quote(member, c.couponId(), total, now);
    var amounts = PriceCalculator.calculate(lineAmounts, coupon.discountFen(), freight);
    String fingerprint =
        Digests.sha256(json.write(new Fingerprint(c, skus, address, coupon, freight)));
    return new CheckoutQuote(
        skus,
        amounts.linePayableFen(),
        address,
        total,
        coupon.discountFen(),
        freight,
        amounts.payableFen(),
        fingerprint,
        now);
  }

  record Fingerprint(
      CheckoutCommand command,
      List<SkuSnapshot> skus,
      AddressSnapshot address,
      CouponQuote coupon,
      long freightFen) {}
}
