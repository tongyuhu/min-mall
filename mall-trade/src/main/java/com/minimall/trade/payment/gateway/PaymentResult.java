package com.minimall.trade.payment.gateway;import java.time.Instant;import java.util.Map;
public record PaymentResult(String paymentNo,String channelTransactionNo,PaymentState status,Instant paidAt,long amountFen,String currency,Map<String,String> payParameters){}
