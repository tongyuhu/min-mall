package com.minimall.trade.shipping.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.DbRow;
import com.minimall.common.util.*;
import com.minimall.trade.order.persistence.OrderRepository;
import com.minimall.trade.shipping.gateway.*;
import com.minimall.trade.shipping.persistence.ShippingRepository;
import com.minimall.trade.task.application.IntegrationTaskService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {
  private final ShippingRepository shipments;
  private final OrderRepository orders;
  private final IntegrationTaskService tasks;
  private final Json json;
  private final Clock clock;

  public ShippingService(
      ShippingRepository shipments,
      OrderRepository orders,
      IntegrationTaskService tasks,
      Json json,
      Clock clock) {
    this.shipments = shipments;
    this.orders = orders;
    this.tasks = tasks;
    this.json = json;
    this.clock = clock;
  }

  @Transactional
  public ShipmentView ship(String orderNo, String carrier, String trackingNo) {
    carrier = Checks.text(carrier, 1, 64, "承运商");
    trackingNo = Checks.text(trackingNo, 1, 100, "运单号");
    var order = orders.byNo(orderNo, true);
    var previous = shipments.byOrder(orderNo);
    if (previous.isPresent()) {
      if (!previous.get().text("carrier").equals(carrier)
          || !previous.get().text("tracking_no").equals(trackingNo))
        throw BusinessException.conflict("订单已有不同运单");
      return view(previous.get());
    }
    Checks.state(
        "PAID".equals(order.text("status")) && order.text("active_after_sale_no") == null,
        "订单不能发货或正在售后");
    String no = BusinessNumbers.next("S");
    shipments.insert(no, orderNo, carrier, trackingNo);
    orders.transition(orderNo, "PAID", "SHIPPED", clock.instant(), "管理端整单发货");
    tasks.enqueue("CREATE_SHIPMENT", no, json.write(new ShipmentTaskCommand(no)));
    return view(shipments.get(no, false));
  }

  public ShipmentRequest creationRequest(String no) {
    var row = shipments.get(no, false);
    return new ShipmentRequest(
        no,
        row.text("order_no"),
        row.text("carrier"),
        row.text("tracking_no"),
        orders.view(orders.byNo(row.text("order_no"), false)).address());
  }

  @Transactional
  public void accept(ShipmentResult result) {
    Checks.state(result.accepted(), "物流渠道未受理");
    shipments.get(result.shipmentNo(), true);
    shipments.accepted(result.shipmentNo());
    tasks.enqueue(
        "QUERY_SHIPMENT",
        result.shipmentNo(),
        json.write(new ShipmentTaskCommand(result.shipmentNo())));
  }

  @Transactional
  public void applyTracks(String no, List<ShipmentTrack> tracks) {
    shipments.get(no, true);
    if (tracks == null || tracks.size() > 500) throw BusinessException.bad("物流轨迹数量不合法");
    for (var t : tracks) {
      Checks.text(t.eventId(), 1, 100, "轨迹事件号");
      Checks.text(t.description(), 1, 255, "轨迹描述");
      if (t.eventTime() == null) throw BusinessException.bad("缺少轨迹时间");
      int stage =
          switch (t.status()) {
            case "CREATED" -> 0;
            case "IN_TRANSIT" -> 1;
            case "DELIVERED" -> 2;
            default -> throw BusinessException.bad("物流状态不合法");
          };
      var previous = shipments.event(no, t.eventId());
      if (previous.isPresent()) {
        var p = previous.get();
        if (!p.text("status").equals(t.status())
            || !p.text("description").equals(t.description())
            || !p.instant("event_time").equals(t.eventTime().truncatedTo(ChronoUnit.MICROS)))
          throw BusinessException.conflict("轨迹事件内容不一致");
        continue;
      }
      shipments.track(no, t);
      shipments.stage(no, stage, t.status());
    }
  }

  @Transactional
  public void confirmReceipt(long member, String no) {
    var order = orders.byNo(no, true);
    if (order.number("member_id") != member) throw BusinessException.missing();
    if ("COMPLETED".equals(order.text("status"))) return;
    Checks.state(
        "SHIPPED".equals(order.text("status")) && order.text("active_after_sale_no") == null,
        "当前不能确认收货");
    orders.transition(no, "SHIPPED", "COMPLETED", clock.instant(), "买家确认收货");
  }

  public List<ShipmentTrack> tracks(long member, String order) {
    orders.owned(member, order);
    return shipments
        .byOrder(order)
        .map(r -> shipments.tracks(r.text("shipment_no")))
        .orElse(List.of());
  }

  public ShipmentView get(String no) {
    return view(shipments.get(no, false));
  }

  public Optional<ShipmentView> byOrder(String no) {
    return shipments.byOrder(no).map(this::view);
  }

  @Transactional
  public void requestSync(String no) {
    shipments.get(no, false);
    tasks.enqueue("QUERY_SHIPMENT", no, json.write(new ShipmentTaskCommand(no)));
  }

  private ShipmentView view(DbRow r) {
    return new ShipmentView(
        r.text("shipment_no"),
        r.text("order_no"),
        r.text("carrier"),
        r.text("tracking_no"),
        r.text("status"));
  }
}
