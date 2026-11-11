package com.minimall.trade.task.infrastructure;
import com.minimall.trade.task.application.*;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.beans.factory.annotation.Value;import java.time.*;import java.util.*;import org.slf4j.*;
@Component public class IntegrationTaskWorker {
 private static final Logger LOG=LoggerFactory.getLogger(IntegrationTaskWorker.class);private final IntegrationTaskService tasks;private final Map<String,TaskHandler> handlers=new HashMap<>();private final Clock clock;private final boolean enabled;
 public IntegrationTaskWorker(IntegrationTaskService tasks,List<TaskHandler> handlers,Clock clock,@Value("${mall.jobs.enabled:true}") boolean enabled){this.tasks=tasks;for(var h:handlers)if(this.handlers.put(h.type(),h)!=null)throw new IllegalStateException("重复任务处理器");this.clock=clock;this.enabled=enabled;}
 @Scheduled(fixedDelayString="${mall.jobs.scan-delay-ms:5000}") public void scheduled(){if(enabled)runOnce();}
 public int runOnce(){int count=0;for(int i=0;i<50;i++){var claimed=tasks.claim(handlers.keySet(),clock.instant(),1,Duration.ofSeconds(60));if(claimed.isEmpty())break;var lease=claimed.getFirst();TaskResult result;try{result=handlers.get(lease.type()).execute(lease);}catch(Exception e){LOG.warn("对接任务失败，任务={},类型={}",lease.id(),e.getClass().getSimpleName());result=TaskResult.retry(e.getClass().getSimpleName());}switch(result.disposition()){case DONE->tasks.complete(lease.id(),lease.leaseToken());case WAIT->tasks.reschedule(lease.id(),lease.leaseToken(),result.nextRunAt());case RETRY->tasks.retry(lease.id(),lease.leaseToken(),result.reason());}count++;}return count;}
 public Set<String> supportedTypes(){return Set.copyOf(handlers.keySet());}
}
