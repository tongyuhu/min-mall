package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.common.persistence.Database;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PersistenceTimeIT extends IntegrationTestSupport {
  @Autowired Database database;

  @Test
  void datetimeRoundTripKeepsUtcAndTruncatesToMicroseconds() {
    Instant input = Instant.parse("2026-11-11T05:00:00.123456789Z");
    var row = database.required("SELECT CAST(? AS DATETIME(6)) AS moment", input);
    assertEquals(input.truncatedTo(ChronoUnit.MICROS), row.instant("moment"));
  }
}
