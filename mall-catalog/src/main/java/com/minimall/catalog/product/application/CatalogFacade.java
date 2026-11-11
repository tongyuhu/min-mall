package com.minimall.catalog.product.application;

import com.minimall.catalog.product.persistence.ProductRepository;
import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.DbRow;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CatalogFacade {
  private final ProductRepository repository;

  public CatalogFacade(ProductRepository repository) {
    this.repository = repository;
  }

  public List<SkuSnapshot> loadSaleSkus(List<Long> ids) {
    return load(ids, false);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public List<SkuSnapshot> lockSaleSkus(List<Long> ids) {
    return load(ids, true);
  }

  private List<SkuSnapshot> load(List<Long> ids, boolean lock) {
    if (ids == null || ids.isEmpty() || ids.size() > 50) throw BusinessException.bad("商品数量不合法");
    var sorted = ids.stream().distinct().sorted().toList();
    var lockedParents = new HashMap<Long, DbRow>();
    if (lock) {
      var parents = new TreeSet<Long>();
      for (long id : sorted) parents.add(repository.sku(id).number("spu_id"));
      // 锁读后的状态必须直接使用；普通查询可能仍落在等待锁之前建立的 RR 快照上。
      parents.forEach(id -> lockedParents.put(id, repository.spu(id, true)));
    }
    var result = new ArrayList<SkuSnapshot>();
    for (long id : sorted) {
      var sku = lock ? repository.lockSku(id) : repository.sku(id);
      var spu =
          lock
              ? lockedParents.get(sku.number("spu_id"))
              : repository.spu(sku.number("spu_id"), false);
      if (!sku.flag("enabled") || !spu.flag("on_sale")) throw BusinessException.conflict("商品已下架");
      var images = repository.images(spu.number("id"), lock);
      result.add(
          new SkuSnapshot(
              id,
              spu.number("id"),
              spu.text("title"),
              sku.text("specs"),
              images.isEmpty() ? null : images.getFirst(),
              sku.number("price_fen"),
              spu.number("sale_version")));
    }
    return List.copyOf(result);
  }
}
