package com.minimall.trade.checkout.application;import java.util.List;import com.fasterxml.jackson.annotation.JsonFormat;
public record CheckoutCommand(@JsonFormat(shape=JsonFormat.Shape.STRING) long addressId,List<CheckoutLine> lines,@JsonFormat(shape=JsonFormat.Shape.STRING) Long couponId){}
