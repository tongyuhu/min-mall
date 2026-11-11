package com.minimall.catalog.product.application;
import com.fasterxml.jackson.annotation.JsonFormat;
public record SkuSnapshot(@JsonFormat(shape=JsonFormat.Shape.STRING) long skuId,@JsonFormat(shape=JsonFormat.Shape.STRING) long spuId,String title,String specs,String imageUrl,long priceFen,long saleVersion){}
