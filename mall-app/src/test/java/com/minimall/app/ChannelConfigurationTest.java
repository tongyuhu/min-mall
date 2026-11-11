package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.config.ChannelConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ChannelConfigurationTest {
  @Test
  void productionDoesNotRegisterDevelopmentControllersOrSeeds() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("prod", "local", "test");
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "test", Map.of("mall.simulation.enabled", true, "mall.seed.enabled", true)));
      context.register(
          com.minimall.trade.payment.mock.MockPaymentController.class,
          com.minimall.trade.shipping.mock.MockShippingController.class,
          com.minimall.app.seed.LocalAdminInitializer.class,
          com.minimall.app.seed.LocalDemoInitializer.class);
      context.refresh();
      assertTrue(
          context
              .getBeansOfType(com.minimall.trade.payment.mock.MockPaymentController.class)
              .isEmpty());
      assertTrue(
          context
              .getBeansOfType(com.minimall.trade.shipping.mock.MockShippingController.class)
              .isEmpty());
      assertTrue(
          context.getBeansOfType(org.springframework.boot.ApplicationRunner.class).isEmpty());
    }
  }

  @Test
  void refusesProductionEvenWhenSimulationIsRequested() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("prod", "local");
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "test",
                  Map.of(
                      "mall.simulation.enabled",
                      true,
                      "mall.simulation.key",
                      "integration-only-secret-key-32-bytes")));
      context.register(ChannelConfiguration.class);
      assertThrows(RuntimeException.class, context::refresh);
    }
  }
}
