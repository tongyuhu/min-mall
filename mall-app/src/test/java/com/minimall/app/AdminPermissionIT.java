package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.admin.auth.application.AdminAccountService;
import com.minimall.admin.rbac.application.RoleService;
import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminPermissionIT extends IntegrationTestSupport {
  @Autowired AdminAccountService accounts;
  @Autowired RoleService roles;
  @Autowired SessionService sessions;

  @Test
  void keepsLastSuperAdministratorEnabled() {
    long id = accounts.bootstrap("testadmin", "learning-password");
    assertThrows(BusinessException.class, () -> accounts.setEnabled(id, false));
    assertThrows(BusinessException.class, () -> roles.assign(id, Set.of(2L)));
  }

  @Test
  void permissionsAreLoadedOnEveryRequest() {
    String name = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    long id = accounts.create(name, "learning-password", Set.of(2L));
    var session = sessions.login(ActorType.ADMIN, name, "learning-password", name);
    assertTrue(sessions.authenticate(session.token()).permissions().contains("catalog:write"));
    roles.assign(id, Set.of(3L));
    assertFalse(sessions.authenticate(session.token()).permissions().contains("catalog:write"));
  }
}
