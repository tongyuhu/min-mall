package com.minimall.user.address.application;

import com.minimall.common.api.BusinessException;
import com.minimall.common.persistence.DbRow;
import com.minimall.common.util.Checks;
import com.minimall.user.address.persistence.AddressRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {
  private final AddressRepository repository;

  public AddressService(AddressRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public long save(long memberId, Long id, AddressInput a) {
    if (a == null) throw BusinessException.bad("地址不能为空");
    a =
        new AddressInput(
            Checks.text(a.receiver(), 1, 64, "收件人"),
            Checks.text(a.phone(), 6, 32, "电话"),
            Checks.text(a.province(), 1, 64, "省份"),
            Checks.text(a.city(), 1, 64, "城市"),
            Checks.text(a.district(), 1, 64, "区县"),
            Checks.text(a.detail(), 1, 255, "详细地址"),
            a.defaultAddress());
    repository.lockMember(memberId);
    if (id != null) repository.owned(memberId, id);
    else if (repository.list(memberId).size() >= 20) throw BusinessException.bad("最多保存20个地址");
    if (a.defaultAddress()) repository.clearDefault(memberId);
    if (id == null) return repository.insert(memberId, a);
    repository.update(memberId, id, a);
    return id;
  }

  public List<AddressView> list(long memberId) {
    return repository.list(memberId).stream()
        .map(r -> new AddressView(Long.toString(r.number("id")), snapshot(r)))
        .toList();
  }

  @Transactional
  public void delete(long memberId, long id) {
    repository.lockMember(memberId);
    repository.owned(memberId, id);
    repository.delete(memberId, id);
  }

  public AddressSnapshot snapshot(long memberId, long id) {
    return snapshot(repository.owned(memberId, id));
  }

  private AddressSnapshot snapshot(DbRow r) {
    return new AddressSnapshot(
        r.text("receiver"),
        r.text("phone"),
        r.text("province"),
        r.text("city"),
        r.text("district"),
        r.text("detail"),
        r.flag("default_address"));
  }
}
