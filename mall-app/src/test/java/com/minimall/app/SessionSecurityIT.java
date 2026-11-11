package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.*;
import com.minimall.user.account.application.MemberService;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SessionSecurityIT extends IntegrationTestSupport {
  @Autowired MemberService members;
  @Autowired SessionService sessions;

  @Test
  void revokesOldSessionDespiteRedisRetention() {
    String login = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    long id = members.register(login, "learning-password");
    Instant before = Instant.now();
    var session = sessions.login(ActorType.MEMBER, login, "learning-password", login);
    assertTrue(Duration.between(before, session.expiresAt()).toSeconds() >= 7199);
    assertEquals(id, sessions.authenticate(session.token()).id());
    members.changePassword(id, "learning-password", "another-password");
    assertEquals(
        401,
        assertThrows(BusinessException.class, () -> sessions.authenticate(session.token()))
            .httpStatus());
  }

  @Test
  void logoutInvalidatesToken() {
    String login = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    members.register(login, "learning-password");
    var session = sessions.login(ActorType.MEMBER, login, "learning-password", login);
    sessions.logout(session.token());
    assertThrows(BusinessException.class, () -> sessions.authenticate(session.token()));
  }
}
