package com.minimall.user.account.api;

import com.minimall.common.auth.*;
import com.minimall.user.account.application.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MemberController {
  private final MemberService service;
  private final CurrentActor actor;

  public MemberController(MemberService service, CurrentActor actor) {
    this.service = service;
    this.actor = actor;
  }

  record ProfileInput(String nickname) {}

  record PasswordInput(String oldPassword, String newPassword) {}

  @GetMapping
  public MemberView profile() {
    return service.profile(actor.require(ActorType.MEMBER));
  }

  @PatchMapping
  public void update(@RequestBody ProfileInput input) {
    service.updateProfile(actor.require(ActorType.MEMBER), input.nickname());
  }

  @PutMapping("/password")
  public void password(@RequestBody PasswordInput input) {
    service.changePassword(
        actor.require(ActorType.MEMBER), input.oldPassword(), input.newPassword());
  }
}
