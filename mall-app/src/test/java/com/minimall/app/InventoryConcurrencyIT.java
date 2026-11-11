package com.minimall.app;
import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.catalog.product.application.*;
import com.minimall.catalog.stock.application.*;
import com.minimall.common.api.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class InventoryConcurrencyIT extends IntegrationTestSupport {
 @Autowired ProductService products;@Autowired TaxonomyService taxonomy;@Autowired InventoryService inventory;@Autowired PlatformTransactionManager manager;
 long sku(){long c=taxonomy.save("categories",null,null,"库存测试");long p=products.save(new ProductCommand(null,c,null,"测试商品",List.of(),List.of(new SkuInput(null,List.of(),100))));return Long.parseLong(products.adminDetail(p).skus().getFirst().id());}
 @Test void preventsOversellingUnderContention() throws Exception {long sku=sku();inventory.adjust(sku,10,UUID.randomUUID().toString());var tx=new TransactionTemplate(manager);try(var pool=Executors.newFixedThreadPool(20)){var start=new CountDownLatch(1);var futures=new ArrayList<Future<Boolean>>();for(int i=0;i<20;i++){String order="reserve-"+UUID.randomUUID();futures.add(pool.submit(()->{start.await();try{tx.executeWithoutResult(s->inventory.reserve(order,List.of(new StockLine(sku,1))));return true;}catch(BusinessException e){return false;}}));}start.countDown();int successes=0;for(var f:futures)if(f.get(30,TimeUnit.SECONDS))successes++;assertEquals(10,successes);assertEquals(new StockView(0,10),inventory.get(sku));}}
 @Test void rollsBackAllSkusAndReleasesOnlyOnce(){long first=sku(),second=sku();inventory.adjust(first,2,UUID.randomUUID().toString());var tx=new TransactionTemplate(manager);assertThrows(BusinessException.class,()->tx.executeWithoutResult(s->inventory.reserve("multi-"+UUID.randomUUID(),List.of(new StockLine(first,1),new StockLine(second,1)))));assertEquals(new StockView(2,0),inventory.get(first));String order="once-"+UUID.randomUUID();tx.executeWithoutResult(s->inventory.reserve(order,List.of(new StockLine(first,1))));tx.executeWithoutResult(s->inventory.release(order,List.of(new StockLine(first,1))));tx.executeWithoutResult(s->inventory.release(order,List.of(new StockLine(first,1))));assertEquals(new StockView(2,0),inventory.get(first));}
}
