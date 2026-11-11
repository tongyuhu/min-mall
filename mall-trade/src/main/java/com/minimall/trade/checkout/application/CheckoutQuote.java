package com.minimall.trade.checkout.application;
import java.util.List;import java.time.Instant;import com.minimall.catalog.product.application.SkuSnapshot;import com.minimall.user.address.application.AddressSnapshot;
public record CheckoutQuote(List<SkuSnapshot> skus,List<Long> linePayableFen,AddressSnapshot address,long goodsFen,long discountFen,long freightFen,long payableFen,String fingerprint,Instant quotedAt){}
