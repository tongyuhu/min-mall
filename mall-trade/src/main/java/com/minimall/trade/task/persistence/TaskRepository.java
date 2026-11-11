package com.minimall.trade.task.persistence;

import com.minimall.common.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
  private final Database db;

  public TaskRepository(Database db) {
    this.db = db;
  }

  public long enqueue(String type, String key, String payload, Instant now) {
    db.update(
        "INSERT INTO"
            + " integration_task(type,business_key,payload,status,next_run_at,created_at,wait_started_at)"
            + " VALUES(?,?,?,'READY',?,?,?) ON DUPLICATE KEY UPDATE id=id",
        type,
        key,
        payload,
        now,
        now,
        now);
    return db.required("SELECT id FROM integration_task WHERE type=? AND business_key=?", type, key)
        .number("id");
  }

  public DbRow get(long id, boolean lock) {
    return db.required(
        "SELECT * FROM integration_task WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public List<DbRow> due(Set<String> types, Instant now, int limit) {
    if (types.isEmpty()) return List.of();
    String marks = String.join(",", Collections.nCopies(types.size(), "?"));
    var args = new ArrayList<Object>(types);
    args.add(now);
    args.add(now);
    args.add(limit);
    return db.rows(
        "SELECT * FROM integration_task WHERE type IN ("
            + marks
            + ") AND ((status='READY' AND next_run_at<=?) OR (status='RUNNING' AND lease_until<=?))"
            + " ORDER BY next_run_at,id LIMIT ? FOR UPDATE SKIP LOCKED",
        args.toArray());
  }

  public void claim(long id, String token, Instant until) {
    db.update(
        "UPDATE integration_task SET status='RUNNING',lease_token=?,lease_until=? WHERE id=?",
        token,
        until,
        id);
  }

  public boolean finish(
      long id, String token, String status, Instant next, String error, int attempts, Instant now) {
    return db.update(
            "UPDATE integration_task SET"
                + " status=?,next_run_at=?,last_error=?,attempts=?,lease_token=NULL,lease_until=NULL"
                + " WHERE id=? AND status='RUNNING' AND lease_token=? AND lease_until>?",
            status,
            next,
            error,
            attempts,
            id,
            token,
            now)
        == 1;
  }

  public void reset(long id, Instant now) {
    db.update(
        "UPDATE integration_task SET"
            + " status='READY',next_run_at=?,wait_started_at=?,attempts=0,lease_token=NULL,lease_until=NULL"
            + " WHERE id=?",
        now,
        now,
        id);
  }

  public List<DbRow> list() {
    return db.rows(
        "SELECT * FROM integration_task WHERE status<>'DONE' ORDER BY id DESC LIMIT 200");
  }
}
