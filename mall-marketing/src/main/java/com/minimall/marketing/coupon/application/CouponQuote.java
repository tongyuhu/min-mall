package com.minimall.marketing.coupon.application;import com.fasterxml.jackson.annotation.JsonFormat;
public record CouponQuote(@JsonFormat(shape=JsonFormat.Shape.STRING) Long couponId,long discountFen){}
