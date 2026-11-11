package com.minimall.trade.shipping.task;
import com.minimall.trade.shipping.application.*;import com.minimall.trade.shipping.gateway.ShippingGateway;import com.minimall.trade.task.application.*;import com.minimall.common.util.Json;import org.springframework.stereotype.Component;import java.time.Clock;
@Component public class QueryShipmentHandler implements TaskHandler {
 private final ShippingService shipping;private final ShippingGateway gateway;private final Json json;private final Clock clock;public QueryShipmentHandler(ShippingService shipping,ShippingGateway gateway,Json json,Clock clock){this.shipping=shipping;this.gateway=gateway;this.json=json;this.clock=clock;}
 public String type(){return "QUERY_SHIPMENT";}public TaskResult execute(TaskLease lease){String no=json.read(lease.payload(),ShipmentTaskCommand.class).shipmentNo();shipping.applyTracks(no,gateway.query(no));return "DELIVERED".equals(shipping.get(no).status())?TaskResult.done():TaskResult.waitUntil(clock.instant().plusSeconds(30));}
}
