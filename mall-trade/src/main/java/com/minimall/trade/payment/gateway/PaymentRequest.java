package com.minimall.trade.payment.gateway;import java.time.Instant;import java.util.Map;
public record PaymentRequest(String paymentNo,String orderNo,long amountFen,String currency,Instant expiresAt){}
