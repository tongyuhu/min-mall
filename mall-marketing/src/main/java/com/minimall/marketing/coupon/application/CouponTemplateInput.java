package com.minimall.marketing.coupon.application;import java.time.Instant;
public record CouponTemplateInput(String name,long thresholdFen,long discountFen,int total,Instant validFrom,Instant validUntil){}
