package com.minimall.common.config;
import org.springframework.context.annotation.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;
@Configuration public class ClockConfiguration {
    @Bean public Clock clock(){return Clock.systemUTC();}
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
}
