package com.minimall.user.account.api;
import com.minimall.common.auth.*;import com.minimall.user.account.application.MemberService;import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("/api/v1/auth") public class MemberAuthController {
 private final MemberService members;private final SessionService sessions;private final AuthRateLimiter limiter;
 public MemberAuthController(MemberService members,SessionService sessions,AuthRateLimiter limiter){this.members=members;this.sessions=sessions;this.limiter=limiter;}
 public record Credentials(String username,String password){}
 @PostMapping("/register") public Map<String,String> register(@RequestBody Credentials input,HttpServletRequest request){limiter.check(ActorType.MEMBER,input.username(),request.getRemoteAddr());return Map.of("id",Long.toString(members.register(input.username(),input.password())));}
 @PostMapping("/login") public SessionView login(@RequestBody Credentials input,HttpServletRequest request){return sessions.login(ActorType.MEMBER,input.username(),input.password(),request.getRemoteAddr());}
 @PostMapping("/logout") public void logout(@RequestHeader("Authorization") String authorization){sessions.logout(authorization.substring(7));}
}
