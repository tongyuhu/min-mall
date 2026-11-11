package com.minimall.trade.order.task;

import com.minimall.trade.order.application.OrderCloseService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiryScanner {
  private final OrderCloseService closer;
  private final Clock clock;
  private final boolean enabled;

  public OrderExpiryScanner(
      OrderCloseService closer, Clock clock, @Value("${mall.jobs.enabled:true}") boolean enabled) {
    this.closer = closer;
    this.clock = clock;
    this.enabled = enabled;
  }

  @Scheduled(fixedDelayString = "${mall.jobs.scan-delay-ms:5000}")
  public void scan() {
    if (enabled) closer.requestExpired(clock.instant(), 50);
  }
}
