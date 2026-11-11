package com.minimall.trade.payment.application;import java.util.Map;
public record PaymentView(String paymentNo,String orderNo,String status,long amountFen,Map<String,String> payParameters){}
