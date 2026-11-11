package com.minimall.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 每次测试进程使用独立容器，不连接开发数据库。 */
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(MallFixtures.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4.13"));
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4.12-alpine"))
            .withExposedPorts(6379);
    static { MYSQL.start(); REDIS.start(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        properties.add("spring.data.redis.password", () -> "");
        properties.add("mall.simulation.enabled", () -> true);
        properties.add("mall.simulation.key", () -> "integration-only-secret-key-32-bytes");
        properties.add("mall.jobs.enabled", () -> false);
        properties.add("mall.seed.enabled", () -> false);
    }
}
