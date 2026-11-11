package com.minimall.app.security;
import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.core.env.*;import com.minimall.common.util.*;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class DevKeyFilter extends OncePerRequestFilter {
 private final Environment env;private final Json json;public DevKeyFilter(Environment env,Json json){this.env=env;this.json=json;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{if(req.getRequestURI().startsWith("/dev/")){boolean enabled=env.acceptsProfiles(Profiles.of("local","test"))&&!env.acceptsProfiles(Profiles.of("prod"))&&env.getProperty("mall.simulation.enabled",Boolean.class,false);if(!enabled){SecurityErrors.write(res,json,404,"NOT_FOUND","资源不存在");return;}if(!Digests.equal(env.getProperty("mall.simulation.key"),req.getHeader("X-Dev-Key"))){SecurityErrors.write(res,json,403,"FORBIDDEN","开发密钥无效");return;}}chain.doFilter(req,res);}
}
