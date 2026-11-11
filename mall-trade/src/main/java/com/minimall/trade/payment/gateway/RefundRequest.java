package com.minimall.trade.payment.gateway;import java.time.Instant;import java.util.Map;
public record RefundRequest(String refundNo,String paymentNo,long amountFen){}
