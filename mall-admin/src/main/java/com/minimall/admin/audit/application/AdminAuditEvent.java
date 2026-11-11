package com.minimall.admin.audit.application;

import java.time.Instant;

public record AdminAuditEvent(
    Long actorId,
    String operation,
    String target,
    String outcome,
    int httpStatus,
    String traceId,
    Instant at) {}
