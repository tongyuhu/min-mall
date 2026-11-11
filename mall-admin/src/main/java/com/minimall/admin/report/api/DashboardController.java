package com.minimall.admin.report.api;
import com.minimall.admin.report.application.DashboardService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.time.LocalDate;
@RestController @RequestMapping("/admin/v1/dashboard") @PreAuthorize("hasAuthority('report:read')") public class DashboardController {
 private final DashboardService service;public DashboardController(DashboardService service){this.service=service;}
 @GetMapping public DashboardService.DashboardView get(@RequestParam LocalDate from,@RequestParam LocalDate until){return service.summary(from,until);}
}
