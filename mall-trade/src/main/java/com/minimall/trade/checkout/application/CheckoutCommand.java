package com.minimall.trade.checkout.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

public record CheckoutCommand(
    @JsonFormat(shape = JsonFormat.Shape.STRING) long addressId,
    List<CheckoutLine> lines,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Long couponId) {}
