package com.minimall.trade.cart.api;

import com.minimall.common.auth.*;
import com.minimall.trade.cart.application.*;
import com.minimall.trade.checkout.application.CheckoutLine;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
  private final CartService cart;
  private final CurrentActor actor;

  public CartController(CartService cart, CurrentActor actor) {
    this.cart = cart;
    this.actor = actor;
  }

  public record Add(long skuId, int quantity) {}

  public record Update(int quantity, boolean selected) {}

  public record Selection(List<Long> itemIds) {}

  @GetMapping
  public List<CartItemView> list() {
    return cart.list(actor.require(ActorType.MEMBER));
  }

  @PostMapping
  public void add(@RequestBody Add input) {
    cart.add(actor.require(ActorType.MEMBER), input.skuId(), input.quantity());
  }

  @PutMapping("/{id}")
  public void update(@PathVariable long id, @RequestBody Update input) {
    cart.update(actor.require(ActorType.MEMBER), id, input.quantity(), input.selected());
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable long id) {
    cart.remove(actor.require(ActorType.MEMBER), id);
  }

  @PostMapping("/selected")
  public List<CheckoutLine> selected(@RequestBody Selection input) {
    return cart.selected(actor.require(ActorType.MEMBER), input.itemIds());
  }
}
