package com.minimall.trade.order.application;

public record TradeStatistics(
    long createdOrders,
    long receivedFen,
    long refundedFen,
    long netReceivedFen,
    long waitingShipment,
    long waitingReview,
    long manualTasks) {}
