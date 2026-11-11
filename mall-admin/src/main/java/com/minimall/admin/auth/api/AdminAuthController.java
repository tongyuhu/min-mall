package com.minimall.admin.auth.api;

import com.minimall.admin.auth.application.AdminAccountService;
import com.minimall.common.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/auth")
public class AdminAuthController {
  private final SessionService sessions;
  private final CurrentActor actor;
  private final AdminAccountService accounts;

  public AdminAuthController(
      SessionService sessions, CurrentActor actor, AdminAccountService accounts) {
    this.sessions = sessions;
    this.actor = actor;
    this.accounts = accounts;
  }

  public record Credentials(String username, String password) {}

  public record PasswordChange(String oldPassword, String newPassword) {}

  @PostMapping("/login")
  public SessionView login(@RequestBody Credentials input, HttpServletRequest request) {
    return sessions.login(
        ActorType.ADMIN, input.username(), input.password(), request.getRemoteAddr());
  }

  @PostMapping("/logout")
  public void logout(@RequestHeader("Authorization") String token) {
    sessions.logout(token.substring(7));
  }

  @PutMapping("/password")
  public void password(@RequestBody PasswordChange input) {
    accounts.changePassword(
        actor.require(ActorType.ADMIN), input.oldPassword(), input.newPassword());
  }
}
