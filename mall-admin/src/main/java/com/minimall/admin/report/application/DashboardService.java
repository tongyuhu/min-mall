package com.minimall.admin.report.application;
import com.minimall.trade.order.application.*;import com.minimall.common.api.BusinessException;import org.springframework.stereotype.Service;import java.time.*;import java.time.temporal.ChronoUnit;
@Service public class DashboardService {
 private final TradeQueryService trade;public DashboardService(TradeQueryService trade){this.trade=trade;}
 public record DashboardView(Instant fromInclusive,Instant untilExclusive,TradeStatistics statistics){}
 public DashboardView summary(LocalDate from,LocalDate until){if(from==null||until==null||until.isBefore(from)||ChronoUnit.DAYS.between(from,until)>366)throw BusinessException.bad("统计日期范围为1至367天");ZoneId zone=ZoneId.of("Asia/Shanghai");Instant start=from.atStartOfDay(zone).toInstant(),end=until.plusDays(1).atStartOfDay(zone).toInstant();return new DashboardView(start,end,trade.statistics(start,end));}
}
