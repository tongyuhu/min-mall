package com.minimall.trade.payment.gateway;import java.util.Map;
public interface PaymentGateway {PaymentResult create(PaymentRequest request);PaymentResult query(String paymentNo);PaymentResult close(String paymentNo);RefundResult refund(RefundRequest request);RefundResult queryRefund(String refundNo);VerifiedPayment verify(byte[] rawBody,Map<String,String> headers);}
