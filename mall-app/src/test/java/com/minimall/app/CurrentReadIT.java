package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.product.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.marketing.coupon.application.*;
import com.minimall.trade.cart.application.CartService;
import com.minimall.trade.checkout.application.CheckoutLine;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 用嵌套的独立事务确定性复现：旧快照建立后，另一事务已经提交新状态。 */
class CurrentReadIT extends IntegrationTestSupport {
  @Autowired com.minimall.user.address.application.AddressService addresses;
  @Autowired MallFixtures f;
  @Autowired CatalogFacade catalog;
  @Autowired ProductService products;
  @Autowired CartService carts;
  @Autowired CouponService coupons;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;

  TransactionTemplate outer() {
    var tx = new TransactionTemplate(manager);
    tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    return tx;
  }

  void committed(Runnable work) {
    var tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    tx.executeWithoutResult(s -> work.run());
  }

  @Test
  void rejectsProductUnlistedAfterTheTransactionSnapshot() {
    var goods = f.goods(100, 5);
    assertThrows(
        BusinessException.class,
        () ->
            outer()
                .executeWithoutResult(
                    s -> {
                      jdbc.queryForObject(
                          "SELECT on_sale FROM product_spu WHERE id=?",
                          Boolean.class,
                          goods.productId());
                      committed(() -> products.setOnSale(goods.productId(), false));
                      catalog.lockSaleSkus(List.of(goods.skuId()));
                    }));
  }

  @Test
  void addingUsesTheLatestCartQuantity() {
    var buyer = f.buyer();
    var goods = f.goods(100, 5);
    carts.add(buyer.memberId(), goods.skuId(), 1);
    outer()
        .executeWithoutResult(
            s -> {
              assertEquals(1, carts.list(buyer.memberId()).getFirst().quantity());
              committed(() -> carts.add(buyer.memberId(), goods.skuId(), 1));
              carts.add(buyer.memberId(), goods.skuId(), 1);
            });
    assertEquals(3, carts.list(buyer.memberId()).getFirst().quantity());
  }

  @Test
  void consumingKeepsItemsAddedAfterTheSnapshot() {
    var buyer = f.buyer();
    var goods = f.goods(100, 5);
    carts.add(buyer.memberId(), goods.skuId(), 1);
    outer()
        .executeWithoutResult(
            s -> {
              carts.list(buyer.memberId());
              committed(() -> carts.add(buyer.memberId(), goods.skuId(), 1));
              carts.consume(buyer.memberId(), List.of(new CheckoutLine(goods.skuId(), 1)));
            });
    assertEquals(1, carts.list(buyer.memberId()).getFirst().quantity());
  }

  @Test
  void newlyDisabledTemplateCannotBeReservedFromAnOldSnapshot() {
    var buyer = f.buyer();
    long template =
        coupons.saveTemplate(
            null,
            new CouponTemplateInput(
                "并发停用",
                0,
                10,
                5,
                clock.instant().minusSeconds(1),
                clock.instant().plusSeconds(3600)));
    long id = coupons.claim(buyer.memberId(), template);
    assertThrows(
        BusinessException.class,
        () ->
            outer()
                .executeWithoutResult(
                    s -> {
                      coupons.quote(buyer.memberId(), id, 100, clock.instant());
                      committed(() -> coupons.setEnabled(template, false));
                      coupons.reserve(buyer.memberId(), id, f.unique(), 100, clock.instant());
                    }));
    assertEquals("AVAILABLE", coupons.mine(buyer.memberId()).getFirst().status());
  }

  @Test
  void orderAddressUsesTheLatestCommittedSnapshot() {
    var buyer = f.buyer();
    outer()
        .executeWithoutResult(
            s -> {
              addresses.snapshot(buyer.memberId(), buyer.addressId());
              committed(
                  () ->
                      addresses.save(
                          buyer.memberId(),
                          buyer.addressId(),
                          new com.minimall.user.address.application.AddressInput(
                              "测试人", "13800000000", "浙江", "杭州", "西湖", "更新地址", true)));
              assertEquals(
                  "更新地址", addresses.lockSnapshot(buyer.memberId(), buyer.addressId()).detail());
            });
  }
}
