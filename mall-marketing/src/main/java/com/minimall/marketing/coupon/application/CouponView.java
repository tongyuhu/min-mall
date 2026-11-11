package com.minimall.marketing.coupon.application;

import java.time.Instant;

public record CouponView(
    String id,
    String templateId,
    String name,
    long thresholdFen,
    long discountFen,
    String status,
    Instant validFrom,
    Instant validUntil) {}
