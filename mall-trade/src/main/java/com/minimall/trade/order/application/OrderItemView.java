package com.minimall.trade.order.application;
public record OrderItemView(String skuId,String title,String specs,String imageUrl,int quantity,long unitPriceFen,long payableFen){}
