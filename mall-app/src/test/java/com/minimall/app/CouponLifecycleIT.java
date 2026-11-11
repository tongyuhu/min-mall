package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.common.api.BusinessException;
import com.minimall.marketing.coupon.application.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CouponLifecycleIT extends IntegrationTestSupport {
  @Autowired CouponService coupons;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;

  @Test
  void honorsThresholdAndExpiredRelease() {
    Instant now = clock.instant();
    long template =
        coupons.saveTemplate(
            null,
            new CouponTemplateInput(
                "测试满减", 10000, 1000, 1, now.minusSeconds(60), now.plusSeconds(3600)));
    long user = Math.abs(UUID.randomUUID().getMostSignificantBits() >>> 1);
    long id = coupons.claim(user, template);
    assertThrows(BusinessException.class, () -> coupons.quote(user, id, 9999, now));
    assertEquals(1000, coupons.quote(user, id, 10000, now).discountFen());
    assertThrows(BusinessException.class, () -> coupons.claim(user + 1, template));
    var tx = new TransactionTemplate(manager);
    String order = "coupon-" + UUID.randomUUID();
    tx.executeWithoutResult(s -> coupons.reserve(user, id, order, 10000, now));
    tx.executeWithoutResult(s -> coupons.release(order, now.plusSeconds(3601)));
    assertEquals("EXPIRED", coupons.mine(user).getFirst().status());
  }
}
