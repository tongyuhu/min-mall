package com.minimall.trade.payment.gateway;

import java.time.Instant;

public record PaymentRequest(
    String paymentNo, String orderNo, long amountFen, String currency, Instant expiresAt) {}
