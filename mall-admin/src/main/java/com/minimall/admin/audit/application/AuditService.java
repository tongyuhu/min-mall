package com.minimall.admin.audit.application;
import com.minimall.common.persistence.Database;import com.minimall.common.api.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import java.time.Instant;
@Service public class AuditService {
 private final Database db;public AuditService(Database db){this.db=db;}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void record(AdminAuditEvent event){db.insert("INSERT INTO admin_operation_log(actor_id,operation,target,outcome,http_status,trace_id,created_at) VALUES(?,?,?,?,?,?,?)",event.actorId(),event.operation(),event.target(),event.outcome(),event.httpStatus(),event.traceId(),event.at());}
 public record AuditView(String id,String actorId,String operation,String target,String outcome,int httpStatus,String traceId,Instant at){}
 public PageResult<AuditView> list(PageQuery page){var list=db.rows("SELECT * FROM admin_operation_log ORDER BY id DESC LIMIT ? OFFSET ?",page.size(),page.offset()).stream().map(r->new AuditView(Long.toString(r.number("id")),r.optionalNumber("actor_id")==null?null:Long.toString(r.number("actor_id")),r.text("operation"),r.text("target"),r.text("outcome"),r.integer("http_status"),r.text("trace_id"),r.instant("created_at"))).toList();return new PageResult<>(list,db.count("SELECT COUNT(*) AS value FROM admin_operation_log"),page.page(),page.size());}
}
