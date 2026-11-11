package com.minimall.app;
import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.catalog.product.application.*;
import com.minimall.common.api.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ProductCatalogIT extends IntegrationTestSupport {
 @Autowired ProductService products;@Autowired TaxonomyService taxonomy;@Autowired CatalogFacade catalog;
 @Test void checkoutIgnoresStalePriceCache(){long category=taxonomy.save("categories",null,null,"测试分类");long id=products.save(new ProductCommand(null,category,null,"测试商品",List.of(),List.of(new SkuInput(null,List.of(),1000))));products.setOnSale(id,true);var detail=products.detail(id);long sku=Long.parseLong(detail.skus().getFirst().id());products.save(new ProductCommand(id,category,null,"测试商品",List.of(),List.of(new SkuInput(sku,List.of(),1200))));assertEquals(1200,catalog.loadSaleSkus(List.of(sku)).getFirst().priceFen());products.setOnSale(id,false);assertThrows(BusinessException.class,()->catalog.loadSaleSkus(List.of(sku)));}
 @Test void rejectsDuplicateSpecificationCombinations(){long category=taxonomy.save("categories",null,null,"测试分类");assertThrows(BusinessException.class,()->products.save(new ProductCommand(null,category,null,"重复SKU",List.of(),List.of(new SkuInput(null,List.of(),100),new SkuInput(null,List.of(),200)))));}
}
