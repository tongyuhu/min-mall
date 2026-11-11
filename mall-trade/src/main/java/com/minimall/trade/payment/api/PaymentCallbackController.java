package com.minimall.trade.payment.api;

import com.minimall.trade.payment.application.PaymentNotificationService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/callbacks/v1/payments")
public class PaymentCallbackController {
  private final PaymentNotificationService notifications;

  public PaymentCallbackController(PaymentNotificationService notifications) {
    this.notifications = notifications;
  }

  @PostMapping("/mock")
  public Map<String, String> accept(
      @RequestBody byte[] body, @RequestHeader Map<String, String> headers) {
    notifications.accept(body, headers);
    return Map.of("result", "SUCCESS");
  }
}
