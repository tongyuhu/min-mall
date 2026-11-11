package com.minimall.common.api;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.slf4j.MDC;
import java.io.IOException; import java.util.UUID;
@Component @Order(-200)
public class TraceIdFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String id=request.getHeader("X-Request-Id");
        if(id==null || !id.matches("[a-zA-Z0-9_-]{1,64}"))id=UUID.randomUUID().toString();
        MDC.put("traceId",id); response.setHeader("X-Request-Id",id);
        try {chain.doFilter(request,response);} finally {MDC.remove("traceId");}
    }
}
