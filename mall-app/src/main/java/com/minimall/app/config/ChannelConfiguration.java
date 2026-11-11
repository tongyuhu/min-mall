package com.minimall.app.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import jakarta.annotation.PostConstruct;
@Configuration
public class ChannelConfiguration {
    private final Environment environment;
    public ChannelConfiguration(Environment environment) { this.environment=environment; }
    @PostConstruct void validate() {
        boolean local=environment.acceptsProfiles(Profiles.of("local", "test"));
        boolean production=environment.acceptsProfiles(Profiles.of("prod"));
        boolean enabled=environment.getProperty("mall.simulation.enabled", Boolean.class, false);
        if(production || !local || !enabled) throw new IllegalStateException("尚未接入真实渠道；仅允许显式开启模拟的 local/test 环境启动");
        if(environment.getProperty("mall.simulation.key", "").getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32)
            throw new IllegalStateException("开发渠道密钥至少需要32字节");
    }
}
