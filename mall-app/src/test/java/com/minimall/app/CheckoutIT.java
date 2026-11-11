package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.common.api.BusinessException;
import com.minimall.trade.checkout.application.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckoutIT extends IntegrationTestSupport {
  @Autowired CheckoutService checkout;
  @Autowired MallFixtures fixtures;

  @Test
  void derivesTotalAndRejectsForeignAddress() {
    var buyer = fixtures.buyer();
    var other = fixtures.buyer();
    var goods = fixtures.goods(100, 5);
    var command =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 2)), null);
    assertEquals(200, checkout.preview(buyer.memberId(), command).payableFen());
    assertThrows(BusinessException.class, () -> checkout.preview(other.memberId(), command));
    assertThrows(
        BusinessException.class,
        () ->
            checkout.preview(
                buyer.memberId(),
                new CheckoutCommand(
                    buyer.addressId(),
                    List.of(
                        new CheckoutLine(goods.skuId(), 60), new CheckoutLine(goods.skuId(), 60)),
                    null)));
  }
}
