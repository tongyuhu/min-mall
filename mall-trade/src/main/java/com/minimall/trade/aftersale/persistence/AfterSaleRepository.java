package com.minimall.trade.aftersale.persistence;
import com.minimall.common.persistence.*;import com.minimall.common.api.PageQuery;import org.springframework.stereotype.Repository;import java.util.*;import java.time.Instant;
@Repository public class AfterSaleRepository {
 private final Database db;public AfterSaleRepository(Database db){this.db=db;}
 public DbRow byNo(String no,boolean lock){return db.required("SELECT * FROM after_sale WHERE after_sale_no=?"+(lock?" FOR UPDATE":""),no);}
 public DbRow byId(long id){return db.required("SELECT * FROM after_sale WHERE id=?",id);}
 public long insert(String no,String order,long member,String type,String reason,Instant now){return db.insert("INSERT INTO after_sale(after_sale_no,order_no,member_id,type,status,reason,created_at) VALUES(?,?,?,?,'WAIT_REVIEW',?,?)",no,order,member,type,reason,now);}
 public void status(String no,String status){db.update("UPDATE after_sale SET status=? WHERE after_sale_no=?",status,no);}
 public void returned(String no,String carrier,String tracking){db.update("UPDATE after_sale SET return_carrier=?,return_tracking_no=?,status='WAIT_INSPECTION' WHERE after_sale_no=?",carrier,tracking,no);}
 public void accepted(String no,Instant at){db.update("UPDATE after_sale SET return_received=1,accepted_at=? WHERE after_sale_no=?",at,no);}
 public void refund(String no,String refundNo){db.update("UPDATE after_sale SET refund_no=? WHERE after_sale_no=?",refundNo,no);}
 public void log(String no,String action,String actorType,Long actorId,String reason){db.insert("INSERT INTO after_sale_log(after_sale_no,action,actor_type,actor_id,reason) VALUES(?,?,?,?,?)",no,action,actorType,actorId,reason);}
 public List<DbRow> mine(long member){return db.rows("SELECT * FROM after_sale WHERE member_id=? ORDER BY id DESC LIMIT 200",member);}
 public List<DbRow> search(String status,PageQuery page){return status==null?db.rows("SELECT * FROM after_sale ORDER BY id DESC LIMIT ? OFFSET ?",page.size(),page.offset()):db.rows("SELECT * FROM after_sale WHERE status=? ORDER BY id DESC LIMIT ? OFFSET ?",status,page.size(),page.offset());}
 public long count(String status){return status==null?db.count("SELECT COUNT(*) AS value FROM after_sale"):db.count("SELECT COUNT(*) AS value FROM after_sale WHERE status=?",status);}
}
