package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.admin.auth.application.AdminAccountService;
import com.minimall.app.support.*;
import com.minimall.common.auth.*;
import com.minimall.trade.checkout.application.*;
import com.minimall.trade.order.application.*;
import com.minimall.trade.payment.application.PaymentService;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class FrontendContractIT extends IntegrationTestSupport {
  @Autowired MallFixtures fixtures;
  @Autowired SessionService sessions;
  @Autowired AdminAccountService admins;
  @Autowired CheckoutService checkout;
  @Autowired OrderService orders;
  @Autowired PaymentService payments;
  @Autowired com.minimall.catalog.product.application.ProductService products;
  @Autowired com.minimall.catalog.product.application.TaxonomyService taxonomy;
  @LocalServerPort int port;

  String token(MallFixtures.Buyer buyer) {
    return sessions
        .login(ActorType.MEMBER, buyer.username(), "learning-password", fixtures.unique())
        .token();
  }

  HttpResponse<String> request(String path, String token, boolean post) throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (post) builder.POST(HttpRequest.BodyPublishers.noBody());
    return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void adminProfileIsAuthenticatedAndContainsOnlySafeFields() throws Exception {
    assertEquals(401, request("/admin/v1/auth/me", null, false).statusCode());
    assertEquals(403, request("/admin/v1/auth/me", token(fixtures.buyer()), false).statusCode());
    String username = "a" + fixtures.unique();
    admins.create(username, "learning-password", Set.of(2L));
    String token =
        sessions.login(ActorType.ADMIN, username, "learning-password", fixtures.unique()).token();
    var response = request("/admin/v1/auth/me", token, false);
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains(username));
    assertTrue(response.body().contains("permissions"));
    assertFalse(response.body().contains("password"));
    assertFalse(response.body().contains("authVersion"));
  }

  @Test
  void simulationRejectsAnonymousAndAnotherBuyer() throws Exception {
    var owner = fixtures.buyer();
    var goods = fixtures.goods(1990, 5);
    var command =
        new CheckoutCommand(owner.addressId(), List.of(new CheckoutLine(goods.skuId(), 1)), null);
    var quote = checkout.preview(owner.memberId(), command);
    var order =
        orders.place(
            owner.memberId(),
            fixtures.unique(),
            new PlaceOrderCommand(command, quote.fingerprint(), false));
    var payment = payments.create(owner.memberId(), order.orderNo());
    String path = "/api/v1/simulation/payments/" + payment.paymentNo() + "/success";
    assertEquals(401, request(path, null, true).statusCode());
    assertEquals(404, request(path, token(fixtures.buyer()), true).statusCode());
    // 支付渠道尚未创建时，应明确拒绝操作，而不是伪造支付成功。
    assertEquals(409, request(path, token(owner), true).statusCode());
  }

  @Test
  void editorKeepsDisabledSkuIdentityForRestoringItsSpecification() {
    var goods = fixtures.goods(1990, 5);
    var original = products.adminDetail(goods.productId());
    long spec = taxonomy.save("specifications", null, null, "颜色");
    long value = taxonomy.save("specification-values", null, spec, "绿色");
    products.save(
        new com.minimall.catalog.product.application.ProductCommand(
            goods.productId(),
            Long.parseLong(original.categoryId()),
            null,
            original.title(),
            List.of(),
            List.of(
                new com.minimall.catalog.product.application.SkuInput(
                    null, List.of(value), 2990))));
    var editor = products.editor(goods.productId());
    assertEquals(2, editor.skus().size());
    assertTrue(
        editor.skus().stream().anyMatch(s -> s.skuId().equals(Long.toString(goods.skuId()))));
    assertFalse(
        editor.skus().stream()
            .filter(s -> s.skuId().equals(Long.toString(goods.skuId())))
            .findFirst()
            .orElseThrow()
            .enabled());
    products.save(
        new com.minimall.catalog.product.application.ProductCommand(
            goods.productId(),
            Long.parseLong(original.categoryId()),
            null,
            original.title(),
            List.of(),
            editor.skus().stream()
                .map(
                    s ->
                        new com.minimall.catalog.product.application.SkuInput(
                            Long.parseLong(s.skuId()),
                            s.specificationValueIds().stream().map(Long::parseLong).toList(),
                            s.priceFen()))
                .toList()));
    assertTrue(products.editor(goods.productId()).skus().stream().allMatch(s -> s.enabled()));
  }
}
