package com.minimall.catalog.product.application;

import java.util.List;

public record ProductCommand(
    Long spuId,
    long categoryId,
    Long brandId,
    String title,
    List<String> imageUrls,
    List<SkuInput> skus) {}
