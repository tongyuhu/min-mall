package com.minimall.trade.order.application;

import com.minimall.user.address.application.AddressSnapshot;
import java.time.Instant;
import java.util.List;

public record OrderView(
    String orderNo,
    String status,
    long goodsFen,
    long discountFen,
    long freightFen,
    long payableFen,
    List<OrderItemView> items,
    AddressSnapshot address,
    Instant createdAt,
    Instant expiresAt,
    Instant paidAt,
    Instant completedAt,
    String activeAfterSaleNo,
    boolean closeRequested) {}
