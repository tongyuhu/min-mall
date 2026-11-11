package com.minimall.user.address.application;

public record AddressInput(
    String receiver,
    String phone,
    String province,
    String city,
    String district,
    String detail,
    boolean defaultAddress) {}
