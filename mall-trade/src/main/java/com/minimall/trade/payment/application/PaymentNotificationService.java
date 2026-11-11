package com.minimall.trade.payment.application;
import com.minimall.trade.payment.gateway.PaymentGateway;import com.minimall.trade.payment.persistence.PaymentRepository;import com.minimall.common.util.*;import org.springframework.stereotype.Service;import org.springframework.dao.DuplicateKeyException;import java.util.Map;
@Service public class PaymentNotificationService {
 private final PaymentGateway gateway;private final PaymentOutcomeService outcomes;private final PaymentRepository payments;private final Json json;
 public PaymentNotificationService(PaymentGateway gateway,PaymentOutcomeService outcomes,PaymentRepository payments,Json json){this.gateway=gateway;this.outcomes=outcomes;this.payments=payments;this.json=json;}
 public void accept(byte[] body,Map<String,String> headers){var verified=gateway.verify(body,headers);try{outcomes.apply(verified);}catch(DuplicateKeyException e){var existing=payments.notification(verified.notificationId());if(existing.isEmpty()||!Digests.equal(existing.get().text("payload_hash"),Digests.sha256(json.write(verified)))||!existing.get().text("payment_no").equals(verified.paymentNo()))throw e;}}
}
