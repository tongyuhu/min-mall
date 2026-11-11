package com.minimall.admin.trade.api;
import com.minimall.trade.order.application.*;import com.minimall.common.api.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.time.Instant;
@RestController @RequestMapping("/admin/v1/orders") @PreAuthorize("hasAuthority('order:read')") public class OrderAdminController {
 private final OrderService orders;private final TradeQueryService query;public OrderAdminController(OrderService orders,TradeQueryService query){this.orders=orders;this.query=query;}
 @GetMapping("/{no}") public OrderView get(@PathVariable String no){return orders.adminDetail(no);}
 @GetMapping public PageResult<OrderView> list(@RequestParam(required=false) String orderNo,@RequestParam(required=false) Long memberId,@RequestParam(required=false) String status,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant until,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return query.search(new OrderFilter(orderNo,memberId,status,from,until),new PageQuery(page,size,"id"));}
}
