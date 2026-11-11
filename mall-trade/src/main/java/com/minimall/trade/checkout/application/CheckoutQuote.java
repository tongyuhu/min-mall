package com.minimall.trade.checkout.application;

import com.minimall.catalog.product.application.SkuSnapshot;
import com.minimall.user.address.application.AddressSnapshot;
import java.time.Instant;
import java.util.List;

public record CheckoutQuote(
    List<SkuSnapshot> skus,
    List<Long> linePayableFen,
    AddressSnapshot address,
    long goodsFen,
    long discountFen,
    long freightFen,
    long payableFen,
    String fingerprint,
    Instant quotedAt) {}
