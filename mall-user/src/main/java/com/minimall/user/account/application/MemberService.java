package com.minimall.user.account.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.auth.PasswordRules;
import com.minimall.common.util.Checks;
import com.minimall.user.account.persistence.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
  private final MemberRepository repository;
  private final PasswordEncoder passwords;

  public MemberService(MemberRepository repository, PasswordEncoder passwords) {
    this.repository = repository;
    this.passwords = passwords;
  }

  @Transactional
  public long register(String username, String password) {
    String login = PasswordRules.login(username);
    PasswordRules.validate(password);
    return repository.insert(login, passwords.encode(password));
  }

  public MemberView profile(long id) {
    var m = repository.find(id).orElseThrow(BusinessException::missing);
    return new MemberView(m.id.toString(), m.username, m.nickname, Boolean.TRUE.equals(m.enabled));
  }

  @Transactional
  public void updateProfile(long id, String nickname) {
    repository.lock(id);
    repository.nickname(id, Checks.text(nickname, 1, 64, "昵称"));
  }

  @Transactional
  public void changePassword(long id, String oldPassword, String newPassword) {
    PasswordRules.validate(newPassword);
    var m = repository.lock(id);
    if (oldPassword == null || !passwords.matches(oldPassword, m.text("password_hash")))
      throw BusinessException.bad("原密码不正确");
    repository.password(id, passwords.encode(newPassword));
  }

  @Transactional
  public void setEnabled(long id, boolean enabled) {
    repository.lock(id);
    repository.enabled(id, enabled);
  }

  public com.minimall.common.api.PageResult<MemberView> search(
      com.minimall.common.api.PageQuery page) {
    return new com.minimall.common.api.PageResult<>(
        repository.page(page).stream()
            .map(
                m ->
                    new MemberView(
                        m.id.toString(), m.username, m.nickname, Boolean.TRUE.equals(m.enabled)))
            .toList(),
        repository.count(),
        page.page(),
        page.size());
  }
}
