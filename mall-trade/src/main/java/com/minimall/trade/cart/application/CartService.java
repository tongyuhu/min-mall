package com.minimall.trade.cart.application;

import com.minimall.catalog.product.application.*;
import com.minimall.common.api.BusinessException;
import com.minimall.common.util.Checks;
import com.minimall.trade.cart.persistence.CartRepository;
import com.minimall.trade.checkout.application.CheckoutLine;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CartService {
  private final CartRepository repository;
  private final CatalogFacade catalog;

  public CartService(CartRepository repository, CatalogFacade catalog) {
    this.repository = repository;
    this.catalog = catalog;
  }

  @Transactional
  public void add(long member, long sku, int quantity) {
    Checks.quantity(quantity);
    catalog.loadSaleSkus(List.of(sku));
    repository.lock(member);
    var previous = repository.bySku(member, sku);
    if (previous.isPresent()) {
      var r = previous.get();
      repository.update(r.number("id"), Checks.quantity(r.integer("quantity") + quantity), true);
    } else {
      if (repository.list(member, true).size() >= 100) throw BusinessException.bad("购物车最多100项");
      repository.insert(member, sku, quantity);
    }
  }

  @Transactional
  public void update(long member, long id, int quantity, boolean selected) {
    Checks.quantity(quantity);
    repository.lock(member);
    repository.owned(member, id, true);
    repository.update(id, quantity, selected);
  }

  @Transactional
  public void remove(long member, long id) {
    repository.lock(member);
    repository.owned(member, id, true);
    repository.remove(id);
  }

  public List<CartItemView> list(long member) {
    return repository.list(member).stream()
        .map(
            r -> {
              SkuSnapshot snapshot = null;
              try {
                snapshot = catalog.loadSaleSkus(List.of(r.number("sku_id"))).getFirst();
              } catch (BusinessException e) {
                if (e.httpStatus() != 404 && e.httpStatus() != 409) throw e;
              }
              return new CartItemView(
                  Long.toString(r.number("id")),
                  Long.toString(r.number("sku_id")),
                  r.integer("quantity"),
                  r.flag("selected"),
                  snapshot != null,
                  snapshot == null ? null : snapshot.title(),
                  snapshot == null ? null : snapshot.specs(),
                  snapshot == null ? null : snapshot.priceFen());
            })
        .toList();
  }

  public List<CheckoutLine> selected(long member, List<Long> ids) {
    if (ids == null
        || ids.isEmpty()
        || ids.size() > 50
        || ids.stream().distinct().count() != ids.size())
      throw BusinessException.bad("请选择1至50项不重复的商品");
    return ids.stream()
        .map(
            id -> {
              var r = repository.owned(member, id);
              Checks.state(r.flag("selected"), "购物车条目未勾选");
              return new CheckoutLine(r.number("sku_id"), r.integer("quantity"));
            })
        .toList();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void consume(long member, List<CheckoutLine> lines) {
    repository.lock(member);
    for (var line : lines) {
      var r =
          repository
              .bySku(member, line.skuId())
              .orElseThrow(() -> BusinessException.conflict("购物车商品已变化"));
      int left = r.integer("quantity") - line.quantity();
      if (left < 0) throw BusinessException.conflict("购物车数量已变化");
      if (left == 0) repository.remove(r.number("id"));
      else repository.update(r.number("id"), left, r.flag("selected"));
    }
  }
}
