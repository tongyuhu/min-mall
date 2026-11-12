package com.minimall.trade.payment.mock;

import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.*;
import com.minimall.trade.payment.application.PaymentService;
import com.minimall.trade.payment.gateway.PaymentResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** 本地学习界面的受控入口：只操作当前买家自己的支付单，不向浏览器暴露开发密钥。 */
@RestController
@RequestMapping("/api/v1/simulation/payments")
@Profile("(local | test) & !prod")
@ConditionalOnProperty(name = "mall.simulation.enabled", havingValue = "true")
public class BuyerSimulationController {
  private final PaymentService payments;
  private final MockPaymentGateway gateway;
  private final CurrentActor actor;

  public BuyerSimulationController(
      PaymentService payments, MockPaymentGateway gateway, CurrentActor actor) {
    this.payments = payments;
    this.gateway = gateway;
    this.actor = actor;
  }

  private void ownedAndReady(String no) {
    var payment = payments.get(actor.require(ActorType.MEMBER), no);
    if (!"LOCAL_SIMULATION".equals(payment.payParameters().get("mode")))
      throw BusinessException.conflict("支付渠道准备中，请稍后重试");
  }

  @PostMapping("/{no}/success")
  public PaymentResult success(@PathVariable String no) {
    ownedAndReady(no);
    return gateway.succeed(no, null, false, 1, 0, 0);
  }

  @PostMapping("/{no}/failure")
  public PaymentResult failure(@PathVariable String no) {
    ownedAndReady(no);
    return gateway.fail(no);
  }
}
