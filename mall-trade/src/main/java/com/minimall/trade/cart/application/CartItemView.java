package com.minimall.trade.cart.application;

public record CartItemView(
    String id,
    String skuId,
    int quantity,
    boolean selected,
    boolean valid,
    String title,
    String specs,
    Long priceFen) {}
