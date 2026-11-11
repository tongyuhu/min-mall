package com.minimall.trade.aftersale.domain;

import com.minimall.common.api.BusinessException;
import java.time.*;
import java.util.Set;

public final class AfterSalePolicy {
  private AfterSalePolicy() {}

  public static void validate(String status, Instant completedAt, Instant now) {
    if (!Set.of("PAID", "SHIPPED", "COMPLETED").contains(status))
      throw BusinessException.conflict("当前订单不支持售后");
    if (status.equals("COMPLETED")
        && (completedAt == null || now.isAfter(completedAt.plus(Duration.ofDays(7)))))
      throw BusinessException.conflict("已超过收货后7天售后期限");
  }
}
