package com.minimall.trade.payment.persistence;
import com.minimall.common.persistence.*;import com.minimall.trade.payment.gateway.*;import com.minimall.common.util.Json;import org.springframework.stereotype.Repository;import java.util.*;
@Repository public class PaymentRepository {
 private final Database db;private final Json json;public PaymentRepository(Database db,Json json){this.db=db;this.json=json;}
 public Optional<DbRow> byOrder(String no){return db.first("SELECT * FROM payment_order WHERE order_no=?",no);}
 public DbRow byNo(String no,boolean lock){return db.required("SELECT * FROM payment_order WHERE payment_no=?"+(lock?" FOR UPDATE":""),no);}
 public void insert(String paymentNo,String orderNo,long amount){db.insert("INSERT INTO payment_order(payment_no,order_no,amount_fen,currency,status) VALUES(?,?,?,'CNY','PENDING')",paymentNo,orderNo,amount);}
 public void parameters(PaymentResult result){db.update("UPDATE payment_order SET pay_parameters_json=?,channel_transaction_no=COALESCE(channel_transaction_no,?) WHERE payment_no=? AND status IN ('PENDING','UNKNOWN')",json.write(result.payParameters()),result.channelTransactionNo(),result.paymentNo());}
 public void success(VerifiedPayment p){db.update("UPDATE payment_order SET status='SUCCEEDED',channel_transaction_no=?,paid_at=?,updated_at=CURRENT_TIMESTAMP(6) WHERE payment_no=?",p.channelTransactionNo(),p.paidAt(),p.paymentNo());}
 public void closed(String no){db.update("UPDATE payment_order SET status='CLOSED' WHERE payment_no=? AND status<>'SUCCEEDED'",no);}
 public Optional<DbRow> notification(String id){return db.first("SELECT * FROM payment_notification WHERE notification_id=?",id);}
 public void notification(String id,String paymentNo,String hash){db.insert("INSERT INTO payment_notification(notification_id,payment_no,payload_hash) VALUES(?,?,?)",id,paymentNo,hash);}
}
