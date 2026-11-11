package com.minimall.app.seed;

import com.minimall.catalog.product.application.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.common.persistence.Database;
import com.minimall.marketing.coupon.application.*;
import com.minimall.user.account.application.MemberService;
import com.minimall.user.address.application.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 幂等的本地演示数据，不会覆盖已经修改的价格、库存或密码。 */
@Component
@Order(10)
@Profile("local & !prod")
@ConditionalOnProperty(name = "mall.seed.enabled", havingValue = "true")
public class LocalDemoInitializer implements ApplicationRunner {
  private final Database db;
  private final TransactionTemplate tx;
  private final MemberService members;
  private final AddressService addresses;
  private final TaxonomyService taxonomy;
  private final ProductService products;
  private final InventoryService stock;
  private final CouponService coupons;
  private final Clock clock;
  private final String password;

  public LocalDemoInitializer(
      Database db,
      PlatformTransactionManager manager,
      MemberService members,
      AddressService addresses,
      TaxonomyService taxonomy,
      ProductService products,
      InventoryService stock,
      CouponService coupons,
      Clock clock,
      @Value("${mall.demo-password:}") String password) {
    this.db = db;
    this.tx = new TransactionTemplate(manager);
    this.members = members;
    this.addresses = addresses;
    this.taxonomy = taxonomy;
    this.products = products;
    this.stock = stock;
    this.coupons = coupons;
    this.clock = clock;
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    tx.executeWithoutResult(
        status -> {
          db.update(
              "INSERT INTO local_seed_state(seed_key,completed) VALUES('demo-v1',0) ON DUPLICATE"
                  + " KEY UPDATE seed_key=seed_key");
          if (db.required(
                  "SELECT completed FROM local_seed_state WHERE seed_key='demo-v1' FOR UPDATE")
              .flag("completed")) return;
          long buyer = members.register("demo_buyer", password);
          addresses.save(
              buyer,
              null,
              new AddressInput("学习买家", "13800000000", "浙江省", "杭州市", "西湖区", "本地演示地址", true));
          long category = taxonomy.save("categories", null, null, "学习用品");
          long product =
              products.save(
                  new ProductCommand(
                      null,
                      category,
                      null,
                      "商城学习笔记本",
                      List.of(),
                      List.of(new SkuInput(null, List.of(), 1990))));
          long sku = Long.parseLong(products.adminDetail(product).skus().getFirst().id());
          stock.adjust(sku, 100, "demo-v1-stock");
          products.setOnSale(product, true);
          coupons.saveTemplate(
              null,
              new CouponTemplateInput(
                  "演示满30减5",
                  3000,
                  500,
                  100,
                  clock.instant().minusSeconds(60),
                  clock.instant().plus(Duration.ofDays(365))));
          db.update(
              "UPDATE local_seed_state SET completed=1,completed_at=? WHERE seed_key='demo-v1'",
              clock.instant());
        });
  }
}
