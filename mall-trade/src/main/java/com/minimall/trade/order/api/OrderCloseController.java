package com.minimall.trade.order.api;
import com.minimall.trade.order.application.OrderCloseService;import com.minimall.common.auth.*;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/v1/orders") public class OrderCloseController {
 private final OrderCloseService closer;private final CurrentActor actor;public OrderCloseController(OrderCloseService closer,CurrentActor actor){this.closer=closer;this.actor=actor;}
 @PostMapping("/{no}/cancel") @ResponseStatus(HttpStatus.ACCEPTED) public void cancel(@PathVariable String no){closer.request(actor.require(ActorType.MEMBER),no);}
}
