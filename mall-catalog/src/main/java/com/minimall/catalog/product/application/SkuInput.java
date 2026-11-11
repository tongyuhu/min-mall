package com.minimall.catalog.product.application;
import java.util.List;
public record SkuInput(Long skuId,List<Long> specificationValueIds,long priceFen) {}
