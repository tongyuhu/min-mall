package com.minimall.trade.shipping.gateway;import com.minimall.user.address.application.AddressSnapshot;
public record ShipmentRequest(String shipmentNo,String orderNo,String carrier,String trackingNo,AddressSnapshot address){}
