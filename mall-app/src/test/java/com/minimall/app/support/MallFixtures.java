package com.minimall.app.support;

import com.minimall.catalog.product.application.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.user.account.application.MemberService;
import com.minimall.user.address.application.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;

/** 用例通过业务服务创建数据，不复用开发账号。 */
public class MallFixtures {
  @Autowired MemberService members;
  @Autowired AddressService addresses;
  @Autowired ProductService products;
  @Autowired TaxonomyService taxonomy;
  @Autowired InventoryService inventory;

  public record Buyer(long memberId, long addressId, String username) {}

  public record Goods(long productId, long skuId) {}

  public String unique() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
  }

  public Buyer buyer() {
    String username = "u" + unique();
    long id = members.register(username, "learning-password");
    long address =
        addresses.save(
            id, null, new AddressInput("测试人", "13800000000", "浙江", "杭州", "西湖", "测试地址", true));
    return new Buyer(id, address, username);
  }

  public Goods goods(long price, int available) {
    long category = taxonomy.save("categories", null, null, "测试分类");
    long product =
        products.save(
            new ProductCommand(
                null,
                category,
                null,
                "测试商品",
                List.of(),
                List.of(new SkuInput(null, List.of(), price))));
    products.setOnSale(product, true);
    long sku = Long.parseLong(products.adminDetail(product).skus().getFirst().id());
    if (available > 0) inventory.adjust(sku, available, unique());
    return new Goods(product, sku);
  }
}
