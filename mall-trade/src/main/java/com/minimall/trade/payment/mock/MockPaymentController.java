package com.minimall.trade.payment.mock;
import com.minimall.trade.payment.gateway.PaymentResult;import org.springframework.web.bind.annotation.*;import org.springframework.context.annotation.Profile;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import java.time.Instant;
@RestController @RequestMapping("/dev/v1/payments") @Profile("(local | test) & !prod") @ConditionalOnProperty(name="mall.simulation.enabled",havingValue="true") public class MockPaymentController {
 private final MockPaymentGateway gateway;public MockPaymentController(MockPaymentGateway gateway){this.gateway=gateway;}
 public record Notice(int copies,int delaySeconds,int failures){}public record Anomaly(Instant paidAt,int copies,int delaySeconds,int failures){}public record Mode(String mode){}public record Resolution(boolean success){}
 @PostMapping("/{no}/success") public PaymentResult success(@PathVariable String no,@RequestBody Notice input){return gateway.succeed(no,null,false,input.copies(),input.delaySeconds(),input.failures());}
 @PostMapping("/{no}/anomaly") public PaymentResult anomaly(@PathVariable String no,@RequestBody Anomaly input){return gateway.succeed(no,input.paidAt(),true,input.copies(),input.delaySeconds(),input.failures());}
 @PostMapping("/{no}/failure") public PaymentResult failure(@PathVariable String no){return gateway.query(no);}
 @PutMapping("/{no}/refund-mode") public void mode(@PathVariable String no,@RequestBody Mode input){gateway.refundMode(no,input.mode());}
 @PostMapping("/refunds/{no}/resolve") public void resolve(@PathVariable String no,@RequestBody Resolution input){gateway.resolveRefund(no,input.success());}
}
