package com.minimall.trade.aftersale.application;

import java.time.Instant;

public record AfterSaleView(
    String id,
    String afterSaleNo,
    String orderNo,
    String type,
    String status,
    String reason,
    String returnCarrier,
    String returnTrackingNo,
    String refundNo,
    Instant createdAt,
    Instant acceptedAt) {}
