package com.minimall.app.config;
import org.springframework.context.annotation.*;
import org.springdoc.core.models.GroupedOpenApi;
import io.swagger.v3.oas.models.*; import io.swagger.v3.oas.models.info.Info; import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration public class OpenApiConfiguration {
    @Bean OpenAPI mallApi(){return new OpenAPI().info(new Info().title("Mini Mall 商城接口").version("1.0")).components(new Components().addSecuritySchemes("Bearer",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")).addSecuritySchemes("开发密钥",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Dev-Key")));}
    @Bean GroupedOpenApi memberApi(){return GroupedOpenApi.builder().group("买家").pathsToMatch("/api/v1/**").build();}
    @Bean GroupedOpenApi adminApi(){return GroupedOpenApi.builder().group("管理").pathsToMatch("/admin/v1/**").build();}
    @Bean GroupedOpenApi callbackApi(){return GroupedOpenApi.builder().group("渠道").pathsToMatch("/callbacks/v1/**","/dev/v1/**").build();}
}
