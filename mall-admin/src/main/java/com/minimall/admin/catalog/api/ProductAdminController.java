package com.minimall.admin.catalog.api;

import com.minimall.catalog.product.application.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1")
@PreAuthorize("hasAuthority('catalog:write')")
public class ProductAdminController {
  private final ProductService products;
  private final TaxonomyService taxonomy;

  public ProductAdminController(ProductService products, TaxonomyService taxonomy) {
    this.products = products;
    this.taxonomy = taxonomy;
  }

  public record SaleInput(boolean onSale) {}

  public record EntryInput(Long id, Long parentId, String name) {}

  @PostMapping("/products")
  public Map<String, String> save(@RequestBody ProductCommand input) {
    return Map.of("id", Long.toString(products.save(input)));
  }

  @GetMapping("/products")
  public com.minimall.common.api.PageResult<ProductView> search(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return products.adminSearch(new com.minimall.common.api.PageQuery(page, size, "id"));
  }

  @GetMapping("/products/{id}")
  public ProductView detail(@PathVariable long id) {
    return products.adminDetail(id);
  }

  @PutMapping("/products/{id}/sale")
  public void onSale(@PathVariable long id, @RequestBody SaleInput input) {
    products.setOnSale(id, input.onSale());
  }

  @GetMapping("/catalog/{kind}")
  public List<TaxonomyService.Entry> list(@PathVariable String kind) {
    return taxonomy.list(kind);
  }

  @PostMapping("/catalog/{kind}")
  public Map<String, String> entry(@PathVariable String kind, @RequestBody EntryInput input) {
    return Map.of(
        "id", Long.toString(taxonomy.save(kind, input.id(), input.parentId(), input.name())));
  }

  @DeleteMapping("/catalog/{kind}/{id}")
  public void archive(@PathVariable String kind, @PathVariable long id) {
    taxonomy.archive(kind, id);
  }
}
