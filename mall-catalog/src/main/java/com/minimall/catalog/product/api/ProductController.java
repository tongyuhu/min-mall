package com.minimall.catalog.product.api;
import com.minimall.catalog.product.application.*;import com.minimall.common.api.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1") public class ProductController {
 private final ProductService products;private final TaxonomyService taxonomy;public ProductController(ProductService products,TaxonomyService taxonomy){this.products=products;this.taxonomy=taxonomy;}
 @GetMapping("/products") public PageResult<ProductView> search(@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long brandId,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long minPriceFen,@RequestParam(required=false) Long maxPriceFen,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="id") String sort){return products.search(new ProductQuery(categoryId,brandId,keyword,minPriceFen,maxPriceFen),new PageQuery(page,size,sort));}
 @GetMapping("/products/{id}") public ProductView detail(@PathVariable long id){return products.detail(id);}
 @GetMapping("/categories") public List<TaxonomyService.Entry> categories(){return taxonomy.list("categories");}
 @GetMapping("/brands") public List<TaxonomyService.Entry> brands(){return taxonomy.list("brands");}
}
