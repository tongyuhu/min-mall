package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.catalog.product.application.*;
import com.minimall.common.api.BusinessException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProductCatalogIT extends IntegrationTestSupport {
  @Autowired ProductService products;
  @Autowired TaxonomyService taxonomy;
  @Autowired CatalogFacade catalog;

  @Test
  void checkoutIgnoresStalePriceCache() {
    long category = taxonomy.save("categories", null, null, "测试分类");
    long id =
        products.save(
            new ProductCommand(
                null,
                category,
                null,
                "测试商品",
                List.of(),
                List.of(new SkuInput(null, List.of(), 1000))));
    products.setOnSale(id, true);
    var detail = products.detail(id);
    long sku = Long.parseLong(detail.skus().getFirst().id());
    products.save(
        new ProductCommand(
            id, category, null, "测试商品", List.of(), List.of(new SkuInput(sku, List.of(), 1200))));
    assertEquals(1200, catalog.loadSaleSkus(List.of(sku)).getFirst().priceFen());
    products.setOnSale(id, false);
    assertThrows(BusinessException.class, () -> catalog.loadSaleSkus(List.of(sku)));
  }

  @Test
  void rejectsDuplicateSpecificationCombinations() {
    long category = taxonomy.save("categories", null, null, "测试分类");
    assertThrows(
        BusinessException.class,
        () ->
            products.save(
                new ProductCommand(
                    null,
                    category,
                    null,
                    "重复SKU",
                    List.of(),
                    List.of(
                        new SkuInput(null, List.of(), 100), new SkuInput(null, List.of(), 200)))));
  }

  @Test
  void priceRangeMustMatchTheSameSku() {
    long category = taxonomy.save("categories", null, null, "价格测试");
    long spec = taxonomy.save("specifications", null, null, "容量");
    long small = taxonomy.save("specification-values", null, spec, "小");
    long large = taxonomy.save("specification-values", null, spec, "大");
    long id =
        products.save(
            new ProductCommand(
                null,
                category,
                null,
                "区间测试",
                List.of(),
                List.of(
                    new SkuInput(null, List.of(small), 100),
                    new SkuInput(null, List.of(large), 1000))));
    products.setOnSale(id, true);
    assertEquals(
        0,
        products
            .search(
                new ProductQuery(category, null, null, 200L, 800L),
                new com.minimall.common.api.PageQuery(1, 20, "id"))
            .total());
  }
}
