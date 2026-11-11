package com.minimall.app.seed;
import com.minimall.admin.auth.application.AdminAccountService;import org.springframework.stereotype.Component;import org.springframework.context.annotation.Profile;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.boot.ApplicationRunner;import org.springframework.boot.ApplicationArguments;import org.springframework.beans.factory.annotation.Value;
@Component @Profile("local & !prod") @ConditionalOnProperty(name="mall.seed.enabled",havingValue="true") public class LocalAdminInitializer implements ApplicationRunner {
 private final AdminAccountService accounts;private final String password;public LocalAdminInitializer(AdminAccountService accounts,@Value("${mall.admin-password:}") String password){this.accounts=accounts;this.password=password;}
 public void run(ApplicationArguments args){accounts.bootstrap("admin",password);}
}
