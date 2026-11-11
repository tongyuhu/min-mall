package com.minimall.catalog.product.infrastructure;

import com.minimall.catalog.product.application.ProductView;
import com.minimall.common.api.BusinessException;
import com.minimall.common.util.Json;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class ProductCache {
  private final StringRedisTemplate redis;
  private final Json json;

  public ProductCache(StringRedisTemplate redis, Json json) {
    this.redis = redis;
    this.json = json;
  }

  public ProductView get(long id, Supplier<ProductView> loader) {
    String key = "mall:product:" + id;
    String value = null;
    try {
      value = redis.opsForValue().get(key);
    } catch (RuntimeException ignored) {
    }
    if ("missing".equals(value)) throw BusinessException.missing();
    if (value != null) {
      try {
        return json.read(value, ProductView.class);
      } catch (RuntimeException ignored) {
      }
    }
    try {
      ProductView result = loader.get();
      try {
        redis
            .opsForValue()
            .set(
                key,
                json.write(result),
                Duration.ofSeconds(ThreadLocalRandom.current().nextInt(300, 361)));
      } catch (RuntimeException ignored) {
      }
      return result;
    } catch (BusinessException e) {
      if (e.httpStatus() == 404)
        try {
          redis.opsForValue().set(key, "missing", Duration.ofSeconds(30));
        } catch (RuntimeException ignored) {
        }
      throw e;
    }
  }

  public void invalidate(long id) {
    Runnable action =
        () -> {
          try {
            redis.delete("mall:product:" + id);
          } catch (RuntimeException ignored) {
          }
        };
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    else action.run();
  }
}
