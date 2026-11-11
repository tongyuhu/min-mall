package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.admin.auth.application.AdminAccountService;
import com.minimall.app.support.*;
import com.minimall.catalog.stock.application.InventoryService;
import com.minimall.common.auth.*;
import com.minimall.marketing.coupon.application.*;
import com.minimall.trade.aftersale.application.AfterSaleService;
import com.minimall.trade.cart.application.CartService;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.*;
import com.minimall.trade.payment.gateway.*;
import com.minimall.trade.payment.mock.MockPaymentGateway;
import com.minimall.trade.refund.application.RefundService;
import com.minimall.trade.shipping.application.ShippingService;
import com.minimall.trade.shipping.gateway.ShipmentTrack;
import com.minimall.trade.shipping.mock.MockShippingGateway;
import com.minimall.trade.task.infrastructure.IntegrationTaskWorker;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 真实数据库及模拟渠道闭环；HTTP 鉴权与演示由其他 IT 和 smoke.ps1 覆盖。 */
class MallJourneyIT extends IntegrationTestSupport {
  @Autowired MallFixtures f;
  @Autowired AdminAccountService admins;
  @Autowired CouponService coupons;
  @Autowired CartService cart;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired MockPaymentGateway paymentGateway;
  @Autowired PaymentOutcomeService outcomes;
  @Autowired ShippingService shipping;
  @Autowired MockShippingGateway shippingGateway;
  @Autowired AfterSaleService aftersales;
  @Autowired RefundService refunds;
  @Autowired InventoryService inventory;
  @Autowired IntegrationTaskWorker worker;
  @Autowired Clock clock;

  @Test
  void completesPurchaseReturnAndRefundJourney() {
    var buyer = f.buyer();
    var goods = f.goods(1990, 5);
    long template =
        coupons.saveTemplate(
            null,
            new CouponTemplateInput(
                "满30减5",
                3000,
                500,
                10,
                clock.instant().minusSeconds(60),
                clock.instant().plusSeconds(3600)));
    long coupon = coupons.claim(buyer.memberId(), template);
    cart.add(buyer.memberId(), goods.skuId(), 2);
    var command =
        new CheckoutCommand(buyer.addressId(), List.of(new CheckoutLine(goods.skuId(), 2)), coupon);
    var quote = checkout.preview(buyer.memberId(), command);
    assertEquals(3480, quote.payableFen());
    var order =
        orders.place(
            buyer.memberId(),
            f.unique(),
            new PlaceOrderCommand(command, quote.fingerprint(), true));
    assertTrue(cart.list(buyer.memberId()).isEmpty());
    assertEquals(2, inventory.get(goods.skuId()).reserved());
    var payment = payments.create(buyer.memberId(), order.orderNo());
    worker.runOnce();
    outcomes.observe(paymentGateway.succeed(payment.paymentNo(), null, false, 2, 0, 0));
    assertEquals("PAID", orders.detail(buyer.memberId(), order.orderNo()).status());
    var shipment = shipping.ship(order.orderNo(), "模拟快递", f.unique());
    worker.runOnce();
    shippingGateway.append(
        shipment.shipmentNo(), new ShipmentTrack(f.unique(), clock.instant(), "DELIVERED", "已签收"));
    shipping.applyTracks(shipment.shipmentNo(), shippingGateway.query(shipment.shipmentNo()));
    shipping.confirmReceipt(buyer.memberId(), order.orderNo());
    var after = aftersales.apply(buyer.memberId(), order.orderNo(), "学习整单退货流程");
    long admin = admins.create("a" + f.unique(), "learning-password", Set.of(1L));
    var identity =
        new AccountIdentity(ActorType.ADMIN, admin, null, true, 0, Set.of("aftersale:review"));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(identity, null, List.of()));
    try {
      aftersales.review(after.afterSaleNo(), true, "同意退货");
      aftersales.submitReturn(buyer.memberId(), after.afterSaleNo(), "模拟快递", f.unique());
      aftersales.acceptReturn(after.afterSaleNo());
    } finally {
      SecurityContextHolder.clearContext();
    }
    worker.runOnce();
    var completed = aftersales.detail(buyer.memberId(), after.afterSaleNo());
    assertEquals("COMPLETED", completed.status());
    assertNotNull(completed.refundNo());
    var refund = refunds.get(buyer.memberId(), completed.refundNo());
    // 再次投递成功结果不得再次返库。
    refunds.applyResult(refund.refundNo(), paymentGateway.queryRefund(refund.refundNo()));
    assertEquals("REFUNDED", orders.detail(buyer.memberId(), order.orderNo()).status());
    assertEquals(quote.payableFen(), refund.amountFen());
    assertEquals("SUCCEEDED", refund.status());
    assertEquals(5, inventory.get(goods.skuId()).available());
    assertEquals(0, inventory.get(goods.skuId()).reserved());
    assertEquals("USED", coupons.mine(buyer.memberId()).getFirst().status());
  }
}
