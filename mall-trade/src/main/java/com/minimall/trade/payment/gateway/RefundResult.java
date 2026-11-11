package com.minimall.trade.payment.gateway;

import java.time.Instant;

public record RefundResult(String refundNo, RefundState status, Instant refundedAt) {}
