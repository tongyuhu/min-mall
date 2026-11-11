package com.minimall.trade.checkout.api;
import com.minimall.trade.checkout.application.*;import com.minimall.common.auth.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/checkout") public class CheckoutController {
 private final CheckoutService checkout;private final CurrentActor actor;public CheckoutController(CheckoutService checkout,CurrentActor actor){this.checkout=checkout;this.actor=actor;}
 @PostMapping public CheckoutQuote preview(@RequestBody CheckoutCommand input){return checkout.preview(actor.require(ActorType.MEMBER),input);}
}
