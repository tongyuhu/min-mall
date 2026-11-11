package com.minimall.marketing.coupon.application;

import com.minimall.common.api.*;
import com.minimall.common.persistence.DbRow;
import com.minimall.common.util.Checks;
import com.minimall.marketing.coupon.persistence.CouponRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CouponService {
  private final CouponRepository repository;
  private final Clock clock;

  public CouponService(CouponRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public long saveTemplate(Long id, CouponTemplateInput input) {
    if (input == null) throw BusinessException.bad("优惠券不能为空");
    Checks.text(input.name(), 1, 100, "券名称");
    if (input.thresholdFen() < 0
        || input.discountFen() <= 0
        || input.total() < 1
        || input.validFrom() == null
        || input.validUntil() == null
        || !input.validFrom().isBefore(input.validUntil())) throw BusinessException.bad("优惠券规则不合法");
    if (id == null) return repository.insertTemplate(input);
    var row = repository.template(id, true);
    if (row.number("issued") > 0) throw BusinessException.conflict("已发放的优惠券只能停用或新建");
    repository.updateTemplate(id, input);
    return id;
  }

  @Transactional
  public void setEnabled(long id, boolean enabled) {
    repository.template(id, true);
    repository.enabled(id, enabled);
  }

  @Transactional
  public long claim(long member, long template) {
    var t = repository.template(template, true);
    Instant now = clock.instant();
    if (!t.flag("enabled")
        || now.isBefore(t.instant("valid_from"))
        || !now.isBefore(t.instant("valid_until"))) throw BusinessException.conflict("优惠券不可领取");
    if (!repository.increment(template)) throw BusinessException.conflict("优惠券已领完");
    return repository.issue(member, t);
  }

  public CouponQuote quote(long member, Long coupon, long goods, Instant at) {
    if (goods < 0) throw BusinessException.bad("商品金额不合法");
    if (coupon == null) return new CouponQuote(null, 0);
    return validate(repository.coupon(member, coupon, false), goods, at);
  }

  private CouponQuote validate(DbRow c, long goods, Instant at) {
    if (!"AVAILABLE".equals(c.text("status"))
        || at.isBefore(c.instant("valid_from"))
        || !at.isBefore(c.instant("valid_until"))
        || !repository.template(c.number("template_id"), false).flag("enabled"))
      throw BusinessException.conflict("优惠券当前不可使用");
    if (goods < c.number("threshold_fen")) throw BusinessException.conflict("未达到优惠券门槛");
    return new CouponQuote(c.number("id"), Math.min(goods, c.number("discount_fen")));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public CouponQuote reserve(long member, long id, String no, long goods, Instant at) {
    var c = repository.coupon(member, id, true);
    if ("RESERVED".equals(c.text("status")) && no.equals(c.text("order_no")))
      return new CouponQuote(id, Math.min(goods, c.number("discount_fen")));
    var q = validate(c, goods, at);
    repository.reserve(id, no);
    repository.log(id, no, "RESERVE");
    return q;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void use(String no) {
    repository
        .byOrder(no)
        .ifPresent(
            c -> {
              if ("USED".equals(c.text("status"))) return;
              Checks.state("RESERVED".equals(c.text("status")), "优惠券未被订单占用");
              repository.status(c.number("id"), "USED", false);
              repository.log(c.number("id"), no, "USE");
            });
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void release(String no, Instant at) {
    repository
        .byOrder(no)
        .ifPresent(
            c -> {
              if (!"RESERVED".equals(c.text("status")))
                throw BusinessException.conflict("已核销优惠券不可释放");
              repository.status(
                  c.number("id"),
                  at.isBefore(c.instant("valid_until")) ? "AVAILABLE" : "EXPIRED",
                  true);
              repository.log(c.number("id"), no, "RELEASE");
            });
  }

  public List<CouponView> mine(long member) {
    return repository.mine(member).stream()
        .map(
            c -> {
              String status = c.text("status");
              if ("AVAILABLE".equals(status) && !clock.instant().isBefore(c.instant("valid_until")))
                status = "EXPIRED";
              return new CouponView(
                  Long.toString(c.number("id")),
                  Long.toString(c.number("template_id")),
                  c.text("name"),
                  c.number("threshold_fen"),
                  c.number("discount_fen"),
                  status,
                  c.instant("valid_from"),
                  c.instant("valid_until"));
            })
        .toList();
  }

  public List<TemplateView> templates() {
    return repository.templates().stream()
        .map(
            c ->
                new TemplateView(
                    Long.toString(c.number("id")),
                    c.text("name"),
                    c.number("threshold_fen"),
                    c.number("discount_fen"),
                    c.integer("total") - c.integer("issued"),
                    c.instant("valid_from"),
                    c.instant("valid_until")))
        .toList();
  }

  public List<AdminTemplateView> adminTemplates() {
    return repository.templates(true).stream()
        .map(
            c ->
                new AdminTemplateView(
                    Long.toString(c.number("id")),
                    c.text("name"),
                    c.number("threshold_fen"),
                    c.number("discount_fen"),
                    c.integer("total"),
                    c.integer("issued"),
                    c.flag("enabled"),
                    c.instant("valid_from"),
                    c.instant("valid_until")))
        .toList();
  }

  public record AdminTemplateView(
      String id,
      String name,
      long thresholdFen,
      long discountFen,
      int total,
      int issued,
      boolean enabled,
      Instant validFrom,
      Instant validUntil) {}

  public record TemplateView(
      String id,
      String name,
      long thresholdFen,
      long discountFen,
      int remaining,
      Instant validFrom,
      Instant validUntil) {}
}
