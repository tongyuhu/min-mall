package com.minimall.trade.task.application;

import java.time.Instant;

public record TaskLease(
    long id,
    String type,
    String businessKey,
    String payload,
    String leaseToken,
    Instant leaseUntil) {}
