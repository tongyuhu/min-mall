package com.minimall.trade.task.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.util.Checks;
import com.minimall.trade.task.persistence.TaskRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class IntegrationTaskService {
  private final TaskRepository repository;
  private final Clock clock;
  private final long maxWaitHours;
  private static final long[] DELAYS = {5, 15, 60, 300, 900};

  public IntegrationTaskService(
      TaskRepository repository, Clock clock, @Value("${mall.jobs.max-wait-hours:24}") long hours) {
    this.repository = repository;
    this.clock = clock;
    this.maxWaitHours = hours;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public long enqueue(String type, String key, String payload) {
    Checks.text(type, 1, 64, "任务类型");
    Checks.text(key, 1, 128, "任务业务键");
    if (payload == null || payload.length() > 65536) throw BusinessException.bad("任务载荷不合法");
    long id = repository.enqueue(type, key, payload, clock.instant());
    var row = repository.get(id, false);
    if (!row.text("payload").equals(payload)) throw BusinessException.conflict("同一任务业务键的内容不一致");
    return id;
  }

  @Transactional
  public List<TaskLease> claim(Set<String> types, Instant now, int limit, Duration lease) {
    if (limit < 1 || limit > 100 || lease.isNegative() || lease.isZero())
      throw BusinessException.bad("任务领取参数不合法");
    var result = new ArrayList<TaskLease>();
    for (var row : repository.due(types, now, limit)) {
      String token = UUID.randomUUID().toString();
      Instant until = now.plus(lease);
      repository.claim(row.number("id"), token, until);
      result.add(
          new TaskLease(
              row.number("id"),
              row.text("type"),
              row.text("business_key"),
              row.text("payload"),
              token,
              until));
    }
    return result;
  }

  @Transactional
  public boolean complete(long id, String token) {
    var r = repository.get(id, true);
    return repository.finish(
        id, token, "DONE", clock.instant(), null, r.integer("attempts"), clock.instant());
  }

  @Transactional
  public boolean retry(long id, String token, String reason) {
    var r = repository.get(id, true);
    int attempts = r.integer("attempts") + 1;
    String error = reason == null ? "执行失败" : reason.substring(0, Math.min(reason.length(), 255));
    return repository.finish(
        id,
        token,
        attempts > DELAYS.length ? "MANUAL" : "READY",
        clock.instant().plusSeconds(DELAYS[Math.min(attempts - 1, DELAYS.length - 1)]),
        error,
        attempts,
        clock.instant());
  }

  @Transactional
  public boolean reschedule(long id, String token, Instant next) {
    var r = repository.get(id, true);
    boolean expired =
        !clock
            .instant()
            .isBefore(r.instant("wait_started_at").plus(Duration.ofHours(maxWaitHours)));
    Instant at =
        next == null || !next.isAfter(clock.instant()) ? clock.instant().plusSeconds(5) : next;
    return repository.finish(
        id,
        token,
        expired ? "MANUAL" : "READY",
        at,
        expired ? "等待渠道结果超时，需要人工处理" : null,
        r.integer("attempts"),
        clock.instant());
  }

  @Transactional
  public void retryManually(long id) {
    var r = repository.get(id, true);
    Checks.state("MANUAL".equals(r.text("status")), "只有人工处理任务允许重试");
    repository.reset(id, clock.instant());
  }

  public List<TaskView> pending() {
    return repository.list().stream()
        .map(
            r ->
                new TaskView(
                    Long.toString(r.number("id")),
                    r.text("type"),
                    r.text("business_key"),
                    r.text("status"),
                    r.integer("attempts"),
                    r.instant("next_run_at"),
                    r.text("last_error")))
        .toList();
  }

  public record TaskView(
      String id,
      String type,
      String businessKey,
      String status,
      int attempts,
      Instant nextRunAt,
      String lastError) {}
}
