package com.minimall.trade.payment.gateway;import java.time.Instant;import java.util.Map;
public record VerifiedPayment(String notificationId,String merchantId,String paymentNo,String channelTransactionNo,long amountFen,String currency,Instant paidAt,PaymentState status){}
