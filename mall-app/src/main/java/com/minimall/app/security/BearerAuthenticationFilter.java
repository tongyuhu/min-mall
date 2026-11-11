package com.minimall.app.security;

import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.*;
import com.minimall.common.util.Json;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerAuthenticationFilter extends OncePerRequestFilter {
  private final SessionService sessions;
  private final Json json;

  public BearerAuthenticationFilter(SessionService sessions, Json json) {
    this.sessions = sessions;
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ")) throw BusinessException.unauthorized();
        var identity = sessions.authenticate(header.substring(7));
        if (identity.type() == ActorType.ADMIN) req.setAttribute("mall.actorId", identity.id());
        var granted = new ArrayList<SimpleGrantedAuthority>();
        granted.add(new SimpleGrantedAuthority("ROLE_" + identity.type()));
        identity.permissions().forEach(p -> granted.add(new SimpleGrantedAuthority(p)));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(identity, null, granted));
      } catch (BusinessException e) {
        SecurityErrors.write(res, json, e.httpStatus(), e.code(), e.getMessage());
        return;
      } catch (org.springframework.dao.DataAccessException e) {
        SecurityErrors.write(res, json, 503, "DEPENDENCY_UNAVAILABLE", "依赖服务暂时不可用");
        return;
      }
    }
    try {
      chain.doFilter(req, res);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
