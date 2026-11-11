package com.minimall.trade.refund.persistence;
import com.minimall.common.persistence.*;import org.springframework.stereotype.Repository;import java.util.*;import java.time.Instant;
@Repository public class RefundRepository {
 private final Database db;public RefundRepository(Database db){this.db=db;}
 public Optional<DbRow> byKey(String key){return db.first("SELECT * FROM refund_order WHERE business_key=?",key);}
 public DbRow byNo(String no,boolean lock){return db.required("SELECT * FROM refund_order WHERE refund_no=?"+(lock?" FOR UPDATE":""),no);}
 public void insert(String no,String key,String order,String payment,Long after,long amount,String reason){db.insert("INSERT INTO refund_order(refund_no,business_key,order_no,payment_no,after_sale_id,amount_fen,status,reason) VALUES(?,?,?,?,?,?,'PENDING',?)",no,key,order,payment,after,amount,reason);}
 public void result(String no,String status,Instant at,String error){db.update("UPDATE refund_order SET status=?,refunded_at=?,last_error=?,channel_refund_no=CASE WHEN ?='SUCCEEDED' THEN refund_no ELSE channel_refund_no END WHERE refund_no=?",status,at,error,status,no);}
 public List<DbRow> byOrder(String order){return db.rows("SELECT * FROM refund_order WHERE order_no=? ORDER BY id",order);}
}
