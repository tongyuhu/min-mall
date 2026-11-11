package com.minimall.admin.trade.api;
import com.minimall.trade.shipping.application.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.Optional;
@RestController @RequestMapping("/admin/v1") @PreAuthorize("hasAuthority('shipment:write')") public class ShippingAdminController {
 private final ShippingService shipping;public ShippingAdminController(ShippingService shipping){this.shipping=shipping;}public record Input(String carrier,String trackingNo){}
 @PostMapping("/orders/{no}/shipment") public ShipmentView ship(@PathVariable String no,@RequestBody Input input){return shipping.ship(no,input.carrier(),input.trackingNo());}
 @GetMapping("/orders/{no}/shipment") public Optional<ShipmentView> get(@PathVariable String no){return shipping.byOrder(no);}
 @PostMapping("/shipments/{no}/sync") public void sync(@PathVariable String no){shipping.requestSync(no);}
}
