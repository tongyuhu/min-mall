package com.minimall.app;
import com.minimall.app.support.IntegrationTestSupport;import com.minimall.admin.report.application.DashboardService;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.web.server.LocalServerPort;import org.springframework.jdbc.core.JdbcTemplate;import java.time.*;import java.net.*;import java.net.http.*;import static org.junit.jupiter.api.Assertions.*;
class AdminOperationsIT extends IntegrationTestSupport {
 @Autowired DashboardService dashboard;@Autowired JdbcTemplate jdbc;@LocalServerPort int port;
 @Test void usesBeijingDayBoundaries(){var result=dashboard.summary(LocalDate.of(2026,11,11),LocalDate.of(2026,11,11));assertEquals(Instant.parse("2026-11-10T16:00:00Z"),result.fromInclusive());assertEquals(Instant.parse("2026-11-11T16:00:00Z"),result.untilExclusive());}
 @Test void rejectsAndAuditsUnauthorizedRetry()throws Exception{var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/admin/v1/tasks/1/retry")).POST(HttpRequest.BodyPublishers.noBody()).build();var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());assertEquals(401,response.statusCode());assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM admin_operation_log WHERE http_status=401",Long.class)>0);}
}
