package com.minimall.trade.payment.gateway;

public record RefundRequest(String refundNo, String paymentNo, long amountFen) {}
