package com.minimall.common.auth;
public interface SessionService {
 SessionView login(ActorType type,String login,String password,String sourceIp);
 AccountIdentity authenticate(String token);
 void logout(String token);
}
