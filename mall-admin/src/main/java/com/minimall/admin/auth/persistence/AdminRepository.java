package com.minimall.admin.auth.persistence;

import com.minimall.common.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {
  private final Database db;

  public AdminRepository(Database db) {
    this.db = db;
  }

  public void guard() {
    db.required("SELECT id FROM admin_role WHERE id=1 FOR UPDATE");
  }

  public Optional<DbRow> login(String login) {
    return db.first("SELECT * FROM admin_account WHERE username=?", login);
  }

  public Optional<DbRow> find(long id) {
    return db.first("SELECT * FROM admin_account WHERE id=?", id);
  }

  public DbRow lock(long id) {
    return db.required("SELECT * FROM admin_account WHERE id=? FOR UPDATE", id);
  }

  public long insert(String login, String hash) {
    return db.insert(
        "INSERT INTO admin_account(username,password_hash,enabled,auth_version) VALUES(?,?,1,0)",
        login,
        hash);
  }

  public boolean superAdmin(long id) {
    return db.count(
            "SELECT COUNT(*) AS value FROM admin_account_role WHERE account_id=? AND role_id=1", id)
        > 0;
  }

  public long enabledSuperCount() {
    return db.count(
        "SELECT COUNT(*) AS value FROM admin_account a JOIN admin_account_role ar ON"
            + " a.id=ar.account_id WHERE ar.role_id=1 AND a.enabled=1");
  }

  public Set<String> permissions(long id) {
    var result = new HashSet<String>();
    db.rows(
            "SELECT DISTINCT p.code FROM admin_permission p JOIN admin_role_permission rp ON"
                + " rp.permission_id=p.id JOIN admin_account_role ar ON ar.role_id=rp.role_id WHERE"
                + " ar.account_id=?",
            id)
        .forEach(r -> result.add(r.text("code")));
    return Set.copyOf(result);
  }

  public void roles(long id, Set<Long> roles) {
    for (Long role : roles) db.required("SELECT id FROM admin_role WHERE id=?", role);
    db.update("DELETE FROM admin_account_role WHERE account_id=?", id);
    for (Long role : roles)
      db.insert("INSERT INTO admin_account_role(account_id,role_id) VALUES(?,?)", id, role);
  }

  public void permissions(long roleId, Set<String> codes) {
    db.required("SELECT id FROM admin_role WHERE id=? FOR UPDATE", roleId);
    var ids = new ArrayList<Long>();
    for (String code : codes)
      ids.add(db.required("SELECT id FROM admin_permission WHERE code=?", code).number("id"));
    db.update("DELETE FROM admin_role_permission WHERE role_id=?", roleId);
    for (Long permission : ids)
      db.insert(
          "INSERT INTO admin_role_permission(role_id,permission_id) VALUES(?,?)",
          roleId,
          permission);
  }

  public void enabled(long id, boolean enabled) {
    db.update(
        "UPDATE admin_account SET"
            + " enabled=?,auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",
        enabled,
        id);
  }

  public void password(long id, String hash) {
    db.update(
        "UPDATE admin_account SET"
            + " password_hash=?,auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE"
            + " id=?",
        hash,
        id);
  }

  public List<DbRow> roles() {
    return db.rows("SELECT id,name FROM admin_role ORDER BY id");
  }

  public List<DbRow> accounts() {
    return db.rows("SELECT id,username,enabled FROM admin_account ORDER BY id LIMIT 100");
  }
}
