package com.minimall.catalog.stock.application;
import com.minimall.catalog.stock.persistence.StockRepository;import com.minimall.catalog.product.application.SkuCreatedEvent;import com.minimall.common.api.BusinessException;import com.minimall.common.util.Checks;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import org.springframework.context.event.EventListener;import java.util.*;
@Service public class InventoryService {
 private final StockRepository repository;public InventoryService(StockRepository repository){this.repository=repository;}
 @EventListener @Transactional(propagation=Propagation.MANDATORY) public void initialize(SkuCreatedEvent event){repository.initialize(event.skuId());}
 public StockView get(long id){var r=repository.get(id,false);return new StockView(r.integer("available"),r.integer("reserved"));}
 @Transactional(propagation=Propagation.MANDATORY) public void reserve(String no,List<StockLine> lines){apply("RESERVE",no,lines);}
 @Transactional(propagation=Propagation.MANDATORY) public void commitReservation(String no,List<StockLine> lines){apply("COMMIT",no,lines);}
 @Transactional(propagation=Propagation.MANDATORY) public void release(String no,List<StockLine> lines){apply("RELEASE",no,lines);}
 @Transactional(propagation=Propagation.MANDATORY) public void restock(String no,List<StockLine> lines){apply("RESTOCK",no,lines);}
 @Transactional public void adjust(long sku,int delta,String key){if(delta==0)return;applyOne("ADJUST",Checks.text(key,1,100,"业务键"),sku,delta);}
 private void apply(String type,String key,List<StockLine> lines){Checks.text(key,1,100,"业务键");if(lines==null||lines.isEmpty()||lines.size()>50)throw BusinessException.bad("库存明细不合法");var sorted=lines.stream().sorted(Comparator.comparingLong(StockLine::skuId)).toList();var ids=new HashSet<Long>();for(var line:sorted){Checks.quantity(line.quantity());if(!ids.add(line.skuId()))throw BusinessException.bad("库存明细SKU重复");}for(var line:sorted)applyOne(type,key,line.skuId(),line.quantity());}
 private void applyOne(String type,String no,long sku,int quantity){var stock=repository.get(sku,true);var previous=repository.log(type,no,sku);if(previous.isPresent()){if(previous.get().integer("quantity")!=quantity)throw BusinessException.conflict("同一库存业务键的数量不一致");return;}
  if(type.equals("COMMIT")||type.equals("RELEASE")){var reservation=repository.log("RESERVE",no,sku).orElseThrow(()->BusinessException.conflict("没有对应的库存占用"));if(reservation.integer("quantity")!=quantity||repository.log(type.equals("COMMIT")?"RELEASE":"COMMIT",no,sku).isPresent())throw BusinessException.conflict("库存占用已处理或数量不一致");}
  int beforeA=stock.integer("available"),beforeR=stock.integer("reserved");long a=beforeA,r=beforeR;
  switch(type){case "RESERVE"->{a-=quantity;r+=quantity;}case "COMMIT"->r-=quantity;case "RELEASE"->{a+=quantity;r-=quantity;}case "RESTOCK","ADJUST"->a+=quantity;default->throw new IllegalStateException("未知库存操作");}
  if(a<0||r<0)throw BusinessException.conflict("库存不足或占用数量不匹配");if(a>Integer.MAX_VALUE||r>Integer.MAX_VALUE)throw BusinessException.bad("库存数量溢出");repository.change(sku,(int)a,(int)r);repository.log(type,no,sku,quantity,beforeA,beforeR,(int)a,(int)r);
 }
}
