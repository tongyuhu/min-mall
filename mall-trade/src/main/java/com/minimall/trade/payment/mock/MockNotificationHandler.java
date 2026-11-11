package com.minimall.trade.payment.mock;

import com.minimall.common.util.Json;
import com.minimall.trade.task.application.*;
import com.minimall.trade.task.persistence.TaskRepository;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("(local | test) & !prod")
@ConditionalOnProperty(name = "mall.simulation.enabled", havingValue = "true")
public class MockNotificationHandler implements TaskHandler {
  private final MockPaymentGateway gateway;
  private final Json json;
  private final Clock clock;
  private final Environment env;
  private final TaskRepository tasks;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public MockNotificationHandler(
      MockPaymentGateway gateway, Json json, Clock clock, Environment env, TaskRepository tasks) {
    this.gateway = gateway;
    this.json = json;
    this.clock = clock;
    this.env = env;
    this.tasks = tasks;
  }

  public String type() {
    return "NOTIFY_PAYMENT";
  }

  public TaskResult execute(TaskLease lease) {
    var payload = json.read(lease.payload(), NotificationEnvelope.class);
    if (clock.instant().isBefore(payload.deliverAt()))
      return TaskResult.waitUntil(payload.deliverAt());
    if (tasks.get(lease.id(), false).integer("attempts") < payload.failures())
      return TaskResult.retry("模拟通知暂时失败");
    byte[] body = json.write(payload.payment()).getBytes(StandardCharsets.UTF_8);
    int port =
        env.getProperty(
            "local.server.port",
            Integer.class,
            env.getProperty("server.port", Integer.class, 18080));
    var request =
        HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/callbacks/v1/payments/mock"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("X-Signature", gateway.sign(body))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() / 100 == 2
          ? TaskResult.done()
          : TaskResult.retry("回调返回HTTP " + response.statusCode());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return TaskResult.retry("通知线程被中断");
    } catch (java.io.IOException e) {
      return TaskResult.retry("通知连接失败");
    }
  }
}
