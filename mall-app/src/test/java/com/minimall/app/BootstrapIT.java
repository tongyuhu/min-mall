package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

class BootstrapIT extends IntegrationTestSupport {
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @LocalServerPort int port;

  @Test
  void loadsLocalApplicationWithDependencies() {
    assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
    redis.opsForValue().set("integration:bootstrap", "ready");
    assertEquals("ready", redis.opsForValue().get("integration:bootstrap"));
    var health =
        RestClient.create("http://localhost:" + port)
            .get()
            .uri("/actuator/health")
            .retrieve()
            .body(java.util.Map.class);
    assertEquals("UP", health.get("status"));
  }
}
