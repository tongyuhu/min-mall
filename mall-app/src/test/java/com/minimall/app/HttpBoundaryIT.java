package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.*;
import com.minimall.common.auth.*;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class HttpBoundaryIT extends IntegrationTestSupport {
  @Autowired MallFixtures fixtures;
  @Autowired SessionService sessions;
  @LocalServerPort int port;

  String token() {
    var buyer = fixtures.buyer();
    return sessions
        .login(ActorType.MEMBER, buyer.username(), "learning-password", fixtures.unique())
        .token();
  }

  @Test
  void missingIdempotencyHeaderIsAClientError() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/orders"))
            .header("Authorization", "Bearer " + token())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("INVALID_ARGUMENT"));
  }

  @Test
  void missingRoutesAndWrongMethodsKeepTheirHttpMeaning() throws Exception {
    String token = token();
    var client = HttpClient.newHttpClient();
    var missing =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/no-such-route"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    assertEquals(404, client.send(missing, HttpResponse.BodyHandlers.discarding()).statusCode());
    var method =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/addresses/1"))
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    assertEquals(405, client.send(method, HttpResponse.BodyHandlers.discarding()).statusCode());
  }
}
