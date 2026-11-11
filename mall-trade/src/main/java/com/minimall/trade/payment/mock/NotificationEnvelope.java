package com.minimall.trade.payment.mock;import java.time.Instant;import com.minimall.trade.payment.gateway.VerifiedPayment;
public record NotificationEnvelope(VerifiedPayment payment,Instant deliverAt,int failures){}
