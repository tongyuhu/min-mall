package com.minimall.catalog.product.application;

public record ProductQuery(
    Long categoryId, Long brandId, String keyword, Long minPriceFen, Long maxPriceFen) {}
