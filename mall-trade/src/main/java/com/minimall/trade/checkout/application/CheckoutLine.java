package com.minimall.trade.checkout.application;
import com.fasterxml.jackson.annotation.JsonFormat;
public record CheckoutLine(@JsonFormat(shape=JsonFormat.Shape.STRING) long skuId,int quantity){}
