package com.minimall.trade.payment.mock;

import com.minimall.trade.payment.gateway.VerifiedPayment;
import java.time.Instant;

public record NotificationEnvelope(VerifiedPayment payment, Instant deliverAt, int failures) {}
