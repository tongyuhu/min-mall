package com.minimall.marketing.coupon.api;
import com.minimall.marketing.coupon.application.*;import com.minimall.common.auth.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/coupons") public class CouponController {
 private final CouponService coupons;private final CurrentActor actor;public CouponController(CouponService coupons,CurrentActor actor){this.coupons=coupons;this.actor=actor;}
 @GetMapping public List<CouponService.TemplateView> templates(){return coupons.templates();}
 @GetMapping("/mine") public List<CouponView> mine(){return coupons.mine(actor.require(ActorType.MEMBER));}
 @PostMapping("/{templateId}/claim") public Map<String,String> claim(@PathVariable long templateId){return Map.of("id",Long.toString(coupons.claim(actor.require(ActorType.MEMBER),templateId)));}
}
