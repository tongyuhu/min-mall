package com.minimall.app.security;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;import org.springframework.http.HttpMethod;import org.springframework.core.env.Environment;
import com.minimall.common.auth.SessionService;import com.minimall.common.util.Json;
@Configuration @EnableMethodSecurity public class SecurityConfiguration {
 @Bean SecurityFilterChain security(HttpSecurity http,SessionService sessions,Json json,Environment environment)throws Exception {
  http.csrf(c->c.disable()).cors(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable()).requestCache(c->c.disable()).sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  http.authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/error").permitAll().requestMatchers(HttpMethod.POST,"/api/v1/auth/register","/api/v1/auth/login","/admin/v1/auth/login").permitAll().requestMatchers(HttpMethod.GET,"/api/v1/products/**","/api/v1/categories/**","/api/v1/brands/**").permitAll().requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll().requestMatchers("/callbacks/v1/**","/dev/v1/**").permitAll().requestMatchers("/admin/v1/**").hasRole("ADMIN").requestMatchers("/api/v1/**").hasRole("MEMBER").anyRequest().denyAll());
  http.exceptionHandling(e->e.authenticationEntryPoint((req,res,error)->SecurityErrors.write(res,json,401,"UNAUTHENTICATED","请重新登录")).accessDeniedHandler((req,res,error)->SecurityErrors.write(res,json,403,"FORBIDDEN","没有操作权限")));
  http.addFilterBefore(new BearerAuthenticationFilter(sessions,json),UsernamePasswordAuthenticationFilter.class);
  http.addFilterBefore(new DevKeyFilter(environment,json),BearerAuthenticationFilter.class);
  return http.build();
 }
}
