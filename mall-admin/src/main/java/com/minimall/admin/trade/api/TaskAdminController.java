package com.minimall.admin.trade.api;
import com.minimall.trade.task.application.IntegrationTaskService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.List;
@RestController @RequestMapping("/admin/v1/tasks") public class TaskAdminController {
 private final IntegrationTaskService tasks;public TaskAdminController(IntegrationTaskService tasks){this.tasks=tasks;}
 @GetMapping @PreAuthorize("hasAnyAuthority('order:read','task:retry')") public List<IntegrationTaskService.TaskView> list(){return tasks.pending();}
 @PostMapping("/{id}/retry") @PreAuthorize("hasAuthority('task:retry')") public void retry(@PathVariable long id){tasks.retryManually(id);}
}
