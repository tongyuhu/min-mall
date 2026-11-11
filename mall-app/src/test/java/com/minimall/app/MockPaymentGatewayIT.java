package com.minimall.app;
import com.minimall.app.support.IntegrationTestSupport;import com.minimall.trade.payment.gateway.*;import com.minimall.trade.payment.mock.MockPaymentGateway;import com.minimall.common.api.BusinessException;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class MockPaymentGatewayIT extends IntegrationTestSupport {
 @Autowired MockPaymentGateway gateway;
 @Test void keepsChannelIdempotentAndClosedOrdersClosed(){String no="test-"+UUID.randomUUID();var request=new PaymentRequest(no,"order",100,"CNY",Instant.now().plusSeconds(100));var a=gateway.create(request);var b=gateway.create(request);assertEquals(a.channelTransactionNo(),b.channelTransactionNo());assertEquals(PaymentState.CLOSED,gateway.close(no).status());assertThrows(BusinessException.class,()->gateway.succeed(no,null,false,1,0,0));String tombstone="tomb-"+UUID.randomUUID();gateway.close(tombstone);assertEquals(PaymentState.CLOSED,gateway.create(new PaymentRequest(tombstone,"order",100,"CNY",Instant.now().plusSeconds(100))).status());}
 @Test void rejectsUnsignedNotification(){assertThrows(BusinessException.class,()->gateway.verify("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),Map.of("X-Signature","bad")));}
}
