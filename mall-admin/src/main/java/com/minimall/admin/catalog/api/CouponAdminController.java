package com.minimall.admin.catalog.api;
import com.minimall.marketing.coupon.application.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/admin/v1/coupons") @PreAuthorize("hasAuthority('catalog:write')") public class CouponAdminController {
 private final CouponService coupons;public CouponAdminController(CouponService coupons){this.coupons=coupons;}
 public record Enabled(boolean enabled){}
 @PostMapping public Map<String,String> create(@RequestBody CouponTemplateInput input){return Map.of("id",Long.toString(coupons.saveTemplate(null,input)));}
 @PutMapping("/{id}") public void update(@PathVariable long id,@RequestBody CouponTemplateInput input){coupons.saveTemplate(id,input);}
 @PutMapping("/{id}/enabled") public void enabled(@PathVariable long id,@RequestBody Enabled input){coupons.setEnabled(id,input.enabled());}
 @GetMapping public List<CouponService.TemplateView> list(){return coupons.templates();}
}
