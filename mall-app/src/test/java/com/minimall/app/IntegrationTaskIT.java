package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.trade.task.application.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class IntegrationTaskIT extends IntegrationTestSupport {
  @Autowired IntegrationTaskService tasks;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;

  @Test
  void rejectsExpiredWorkerCompletion() {
    var tx = new TransactionTemplate(manager);
    String type = "TEST_" + UUID.randomUUID();
    long id = tx.execute(s -> tasks.enqueue(type, "lease", "{}"));
    Instant now = clock.instant();
    var first = tasks.claim(Set.of(type), now, 1, Duration.ofSeconds(1)).getFirst();
    var second =
        tasks.claim(Set.of(type), now.plusSeconds(2), 1, Duration.ofSeconds(60)).getFirst();
    assertEquals(id, second.id());
    assertFalse(tasks.complete(id, first.leaseToken()));
    assertTrue(tasks.complete(id, second.leaseToken()));
  }

  @Test
  void rollbackDoesNotLeaveTask() {
    String type = "TEST_" + UUID.randomUUID();
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              tasks.enqueue(type, "rollback", "{}");
              s.setRollbackOnly();
            });
    assertTrue(tasks.claim(Set.of(type), clock.instant(), 10, Duration.ofSeconds(60)).isEmpty());
  }
}
