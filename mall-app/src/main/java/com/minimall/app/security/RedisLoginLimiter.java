package com.minimall.app.security;
import com.minimall.common.auth.*;import com.minimall.common.api.BusinessException;import com.minimall.common.util.Digests;
import org.springframework.stereotype.Component;import org.springframework.data.redis.core.StringRedisTemplate;import org.springframework.data.redis.core.script.DefaultRedisScript;import java.util.List;import java.util.Locale;
@Component public class RedisLoginLimiter implements AuthRateLimiter {
 private final StringRedisTemplate redis;
 private static final DefaultRedisScript<Long> COUNT=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",Long.class);
 public RedisLoginLimiter(StringRedisTemplate redis){this.redis=redis;}
 public void check(ActorType type,String login,String ip){increment("ip:"+Digests.sha256(ip==null?"unknown":ip),30);increment("account:"+type+":"+Digests.sha256(login==null?"":login.strip().toLowerCase(Locale.ROOT)),5);}
 private void increment(String key,int max){Long n;try{n=redis.execute(COUNT,List.of("mall:limit:"+key));}catch(RuntimeException e){throw BusinessException.unavailable();}if(n==null)throw BusinessException.unavailable();if(n>max)throw new BusinessException("RATE_LIMITED",429,"请求过于频繁，请稍后重试");}
}
