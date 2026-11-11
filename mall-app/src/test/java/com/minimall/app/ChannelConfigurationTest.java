package com.minimall.app;

import com.minimall.app.config.ChannelConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ChannelConfigurationTest {
    @Test void refusesProductionEvenWhenSimulationIsRequested() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod", "local");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "mall.simulation.enabled", true, "mall.simulation.key", "integration-only-secret-key-32-bytes")));
            context.register(ChannelConfiguration.class);
            assertThrows(RuntimeException.class, context::refresh);
        }
    }
}
