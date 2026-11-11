package com.minimall.trade.shipping.persistence;

import com.minimall.common.persistence.*;
import com.minimall.trade.shipping.gateway.ShipmentTrack;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ShippingRepository {
  private final Database db;

  public ShippingRepository(Database db) {
    this.db = db;
  }

  public Optional<DbRow> byOrder(String no) {
    return db.first("SELECT * FROM shipment WHERE order_no=?", no);
  }

  public DbRow get(String no, boolean lock) {
    return db.required(
        "SELECT * FROM shipment WHERE shipment_no=?" + (lock ? " FOR UPDATE" : ""), no);
  }

  public void insert(String no, String order, String carrier, String tracking) {
    db.insert(
        "INSERT INTO shipment(shipment_no,order_no,carrier,tracking_no,status,stage)"
            + " VALUES(?,?,?,?,'CREATING',0)",
        no,
        order,
        carrier,
        tracking);
  }

  public void accepted(String no) {
    db.update(
        "UPDATE shipment SET status='ACCEPTED' WHERE shipment_no=? AND status='CREATING'", no);
  }

  public Optional<DbRow> event(String no, String event) {
    return db.first("SELECT * FROM shipment_track WHERE shipment_no=? AND event_id=?", no, event);
  }

  public void track(String no, ShipmentTrack t) {
    db.insert(
        "INSERT INTO shipment_track(shipment_no,event_id,event_time,status,description)"
            + " VALUES(?,?,?,?,?)",
        no,
        t.eventId(),
        t.eventTime(),
        t.status(),
        t.description());
  }

  public void stage(String no, int stage, String status) {
    db.update(
        "UPDATE shipment SET stage=?,status=? WHERE shipment_no=? AND stage<=?",
        stage,
        status,
        no,
        stage);
  }

  public List<ShipmentTrack> tracks(String no) {
    return db
        .rows("SELECT * FROM shipment_track WHERE shipment_no=? ORDER BY event_time,id", no)
        .stream()
        .map(
            r ->
                new ShipmentTrack(
                    r.text("event_id"),
                    r.instant("event_time"),
                    r.text("status"),
                    r.text("description")))
        .toList();
  }
}
