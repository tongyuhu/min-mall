package com.minimall.trade.shipping.gateway;

import java.time.Instant;

public record ShipmentTrack(String eventId, Instant eventTime, String status, String description) {}
