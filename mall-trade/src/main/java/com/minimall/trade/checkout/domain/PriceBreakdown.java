package com.minimall.trade.checkout.domain;import java.util.List;
public record PriceBreakdown(List<Long> linePayableFen,long goodsFen,long discountFen,long freightFen,long payableFen){public PriceBreakdown{linePayableFen=List.copyOf(linePayableFen);}}
