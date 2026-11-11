package com.minimall.catalog.product.application;
import java.util.List;
public record ProductQuery(Long categoryId,Long brandId,String keyword,Long minPriceFen,Long maxPriceFen) {}
