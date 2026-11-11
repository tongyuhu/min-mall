package com.minimall.trade.order.application;

import com.minimall.trade.checkout.application.CheckoutCommand;

public record PlaceOrderCommand(
    CheckoutCommand checkout, String quoteFingerprint, boolean fromCart) {}
