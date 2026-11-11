package com.minimall.trade.refund.application;
public record EnsureRefundCommand(String businessKey,String orderNo,Long afterSaleId,String reason){}
