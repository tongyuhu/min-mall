package com.minimall.trade.refund.application;import java.time.Instant;
public record RefundView(String refundNo,String orderNo,long amountFen,String status,Instant refundedAt,String lastError){}
