package com.flashsale.inventory.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisStockService {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String RESERVE_KEY_PREFIX = "reserve:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveScript = loadScript("redis/reserve_stock.lua");
    private final DefaultRedisScript<Long> releaseScript = loadScript("redis/release_reserve.lua");
    private final DefaultRedisScript<Long> consumeScript = loadScript("redis/consume_reserve.lua");

    public void initializeStock(UUID saleId, int availableStock) {
        String key = stockKey(saleId);
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(availableStock));
    }

    public int getAvailableStock(UUID saleId) {
        String value = redisTemplate.opsForValue().get(stockKey(saleId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public boolean tryReserve(UUID saleId, UUID orderId, int quantity, long ttlSeconds) {
        Long result = redisTemplate.execute(
                reserveScript,
                List.of(stockKey(saleId), reserveKey(orderId)),
                String.valueOf(quantity),
                String.valueOf(ttlSeconds)
        );
        return result != null && result == 1L;
    }

    public void consumeReserve(UUID orderId) {
        redisTemplate.execute(
                consumeScript,
                List.of(reserveKey(orderId))
        );
    }

    public void releaseReserve(UUID saleId, UUID orderId) {
        redisTemplate.execute(
                releaseScript,
                List.of(stockKey(saleId), reserveKey(orderId))
        );
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

    private DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
