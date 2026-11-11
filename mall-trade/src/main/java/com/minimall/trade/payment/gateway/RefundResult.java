package com.minimall.trade.payment.gateway;import java.time.Instant;import java.util.Map;
public record RefundResult(String refundNo,RefundState status,Instant refundedAt){}
