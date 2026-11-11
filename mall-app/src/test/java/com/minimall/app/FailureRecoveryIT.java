package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.catalog.product.application.ProductService;
import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.*;
import com.minimall.trade.task.application.*;
import com.minimall.trade.task.infrastructure.IntegrationTaskWorker;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;

class FailureRecoveryIT extends IntegrationTestSupport {
  @Autowired IntegrationTaskService tasks;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;
  @Autowired JdbcTemplate jdbc;
  @Autowired MallFixtures f;
  @Autowired SessionService sessions;
  @Autowired ProductService products;

  @Test
  void reconstructedWorkerClaimsExpiredLeaseAndIgnoresDuplicateEnqueue() {
    String type = "TEST_" + UUID.randomUUID();
    var tx = new TransactionTemplate(manager);
    long id = tx.execute(s -> tasks.enqueue(type, "restart", "{}"));
    long repeatedId = tx.execute(s -> tasks.enqueue(type, "restart", "{}"));
    assertEquals(id, repeatedId);
    var old = tasks.claim(Set.of(type), clock.instant(), 1, Duration.ofSeconds(60)).getFirst();
    jdbc.update(
        "UPDATE integration_task SET lease_until=? WHERE id=?",
        java.time.LocalDateTime.ofInstant(
            clock.instant().minusSeconds(1), java.time.ZoneOffset.UTC),
        id);
    var calls = new AtomicInteger();
    TaskHandler handler =
        new TaskHandler() {
          public String type() {
            return type;
          }

          public TaskResult execute(TaskLease lease) {
            calls.incrementAndGet();
            return TaskResult.done();
          }
        };
    var replacement = new IntegrationTaskWorker(tasks, List.of(handler), clock, false);
    assertEquals(1, replacement.runOnce());
    assertFalse(tasks.complete(id, old.leaseToken()));
    assertEquals(0, replacement.runOnce());
    assertEquals(1, calls.get());
  }

  @Test
  void manualRetryGetsANewWaitWindow() {
    String type = "TEST_" + UUID.randomUUID();
    long id =
        new TransactionTemplate(manager).execute(s -> tasks.enqueue(type, "refund-retry", "{}"));
    jdbc.update(
        "UPDATE integration_task SET status='MANUAL',wait_started_at=? WHERE id=?",
        java.time.LocalDateTime.ofInstant(
            clock.instant().minus(Duration.ofDays(2)), java.time.ZoneOffset.UTC),
        id);
    tasks.retryManually(id);
    var lease = tasks.claim(Set.of(type), clock.instant(), 1, Duration.ofSeconds(60)).getFirst();
    assertTrue(tasks.reschedule(id, lease.leaseToken(), clock.instant().plusSeconds(10)));
    assertEquals(
        "READY",
        jdbc.queryForObject("SELECT status FROM integration_task WHERE id=?", String.class, id));
  }

  @Test
  void redisFailureRejectsAuthenticationButCatalogFallsBack() {
    var buyer = f.buyer();
    var goods = f.goods(100, 1);
    var session =
        sessions.login(ActorType.MEMBER, buyer.username(), "learning-password", f.unique());
    var docker = DockerClientFactory.instance().client();
    docker.pauseContainerCmd(REDIS.getContainerId()).exec();
    try {
      var error =
          assertThrows(BusinessException.class, () -> sessions.authenticate(session.token()));
      assertEquals(503, error.httpStatus());
      assertEquals(Long.toString(goods.productId()), products.detail(goods.productId()).id());
    } finally {
      docker.unpauseContainerCmd(REDIS.getContainerId()).exec();
    }
  }
}
