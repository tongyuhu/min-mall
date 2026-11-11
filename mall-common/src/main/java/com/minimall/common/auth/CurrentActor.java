package com.minimall.common.auth;

import com.minimall.common.api.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentActor {
  public AccountIdentity identity() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof AccountIdentity p))
      throw BusinessException.unauthorized();
    return p;
  }

  public long require(ActorType type) {
    var p = identity();
    if (p.type() != type) throw new BusinessException("FORBIDDEN", 403, "身份类型不匹配");
    return p.id();
  }
}
