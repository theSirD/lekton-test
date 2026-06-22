package com.flashsale.inventory.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisStockService {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String RESERVE_KEY_PREFIX = "reserve:";
    private static final String RESERVE_QTY_SUFFIX = ":qty";
    private static final String RESERVE_SALE_SUFFIX = ":sale";

    private final StringRedisTemplate redisTemplate;

    public void initializeStock(UUID saleId, int availableStock) {
        String key = stockKey(saleId);
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(availableStock));
    }

    public int getAvailableStock(UUID saleId) {
        String value = redisTemplate.opsForValue().get(stockKey(saleId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public boolean tryReserve(UUID saleId, UUID orderId, int quantity, long ttlSeconds) {
        String stockKey = stockKey(saleId);
        String current = redisTemplate.opsForValue().get(stockKey);
        if (current == null) {
            return false;
        }
        int available = Integer.parseInt(current);
        if (available < quantity) {
            return false;
        }
        Long remaining = redisTemplate.opsForValue().decrement(stockKey, quantity);
        if (remaining == null || remaining < 0) {
            if (remaining != null && remaining < 0) {
                redisTemplate.opsForValue().increment(stockKey, quantity);
            }
            return false;
        }
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        String reserveKey = reserveKey(orderId);
        redisTemplate.opsForValue().set(reserveKey + RESERVE_QTY_SUFFIX, String.valueOf(quantity), ttl);
        redisTemplate.opsForValue().set(reserveKey + RESERVE_SALE_SUFFIX, saleId.toString(), ttl);
        redisTemplate.opsForValue().set(reserveKey, saleId.toString(), ttl);
        return true;
    }

    public void consumeReserve(UUID orderId) {
        String reserveKey = reserveKey(orderId);
        redisTemplate.delete(reserveKey);
        redisTemplate.delete(reserveKey + RESERVE_QTY_SUFFIX);
        redisTemplate.delete(reserveKey + RESERVE_SALE_SUFFIX);
    }

    public void releaseReserve(UUID saleId, UUID orderId) {
        String reserveKey = reserveKey(orderId);
        String qtyValue = redisTemplate.opsForValue().get(reserveKey + RESERVE_QTY_SUFFIX);
        if (qtyValue != null) {
            redisTemplate.opsForValue().increment(stockKey(saleId), Integer.parseInt(qtyValue));
        }
        redisTemplate.delete(reserveKey);
        redisTemplate.delete(reserveKey + RESERVE_QTY_SUFFIX);
        redisTemplate.delete(reserveKey + RESERVE_SALE_SUFFIX);
    }

    public void adjustStock(UUID saleId, int delta) {
        if (delta > 0) {
            redisTemplate.opsForValue().increment(stockKey(saleId), delta);
        } else if (delta < 0) {
            redisTemplate.opsForValue().decrement(stockKey(saleId), -delta);
        }
    }

    private String stockKey(UUID saleId) {
        return STOCK_KEY_PREFIX + saleId;
    }

    private String reserveKey(UUID orderId) {
        return RESERVE_KEY_PREFIX + orderId;
    }
}
