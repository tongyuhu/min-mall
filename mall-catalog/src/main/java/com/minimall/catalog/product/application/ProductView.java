package com.minimall.catalog.product.application;

import java.util.List;

public record ProductView(
    String id,
    String categoryId,
    String brandId,
    String title,
    boolean onSale,
    long saleVersion,
    List<String> imageUrls,
    List<SkuView> skus) {}
