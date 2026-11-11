package com.minimall.trade.order.application;import java.time.Instant;
public record OrderFilter(String orderNo,Long memberId,String status,Instant from,Instant until){}
