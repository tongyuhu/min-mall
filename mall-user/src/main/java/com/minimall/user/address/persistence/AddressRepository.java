package com.minimall.user.address.persistence;

import com.minimall.common.persistence.*;
import com.minimall.user.address.application.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AddressRepository {
  private final Database db;

  public AddressRepository(Database db) {
    this.db = db;
  }

  public void lockMember(long id) {
    db.required("SELECT id FROM member WHERE id=? FOR UPDATE", id);
  }

  public DbRow owned(long memberId, long id) {
    return db.required("SELECT * FROM member_address WHERE member_id=? AND id=?", memberId, id);
  }

  public List<DbRow> list(long memberId) {
    return db.rows(
        "SELECT * FROM member_address WHERE member_id=? ORDER BY default_address DESC,id DESC",
        memberId);
  }

  public void clearDefault(long memberId) {
    db.update("UPDATE member_address SET default_address=0 WHERE member_id=?", memberId);
  }

  public long insert(long memberId, AddressInput a) {
    return db.insert(
        "INSERT INTO"
            + " member_address(member_id,receiver,phone,province,city,district,detail,default_address)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        memberId,
        a.receiver(),
        a.phone(),
        a.province(),
        a.city(),
        a.district(),
        a.detail(),
        a.defaultAddress());
  }

  public void update(long memberId, long id, AddressInput a) {
    db.update(
        "UPDATE member_address SET"
            + " receiver=?,phone=?,province=?,city=?,district=?,detail=?,default_address=? WHERE"
            + " member_id=? AND id=?",
        a.receiver(),
        a.phone(),
        a.province(),
        a.city(),
        a.district(),
        a.detail(),
        a.defaultAddress(),
        memberId,
        id);
  }

  public void delete(long memberId, long id) {
    db.update("DELETE FROM member_address WHERE member_id=? AND id=?", memberId, id);
  }
}
