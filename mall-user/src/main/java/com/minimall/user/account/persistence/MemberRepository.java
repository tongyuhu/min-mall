package com.minimall.user.account.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.common.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {
  private final MemberMapper mapper;
  private final Database db;

  public MemberRepository(MemberMapper mapper, Database db) {
    this.mapper = mapper;
    this.db = db;
  }

  public long insert(String login, String hash) {
    var e = new MemberEntity();
    e.username = login;
    e.passwordHash = hash;
    e.nickname = login;
    e.enabled = true;
    e.authVersion = 0L;
    mapper.insert(e);
    return e.id;
  }

  public Optional<MemberEntity> find(long id) {
    return Optional.ofNullable(mapper.selectById(id));
  }

  public Optional<MemberEntity> login(String login) {
    return Optional.ofNullable(
        mapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", login)));
  }

  public DbRow lock(long id) {
    return db.required("SELECT * FROM member WHERE id=? FOR UPDATE", id);
  }

  public void nickname(long id, String value) {
    db.update("UPDATE member SET nickname=?,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?", value, id);
  }

  public void password(long id, String value) {
    db.update(
        "UPDATE member SET"
            + " password_hash=?,auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE"
            + " id=?",
        value,
        id);
  }

  public void enabled(long id, boolean value) {
    db.update(
        "UPDATE member SET enabled=?,auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP(6)"
            + " WHERE id=?",
        value,
        id);
  }

  public java.util.List<MemberEntity> page(com.minimall.common.api.PageQuery page) {
    return mapper
        .selectPage(
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MemberEntity>(
                page.page(), page.size()),
            new QueryWrapper<MemberEntity>().orderByDesc("id"))
        .getRecords();
  }

  public long count() {
    return mapper.selectCount(null);
  }
}
