package com.minimall.common.auth;
public interface AuthRateLimiter {void check(ActorType type,String login,String sourceIp);}
