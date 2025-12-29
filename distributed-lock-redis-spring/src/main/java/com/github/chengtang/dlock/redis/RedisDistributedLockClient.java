package com.github.chengtang.dlock.redis;

import com.github.chengtang.dlock.annotation.SpinWaitStrategy;
import com.github.chengtang.dlock.core.DistributedLockClient;
import com.github.chengtang.dlock.core.SpinWaitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisDistributedLockClient implements DistributedLockClient {
    private static final String RELEASE_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> releaseScript;
    private final ThreadLocal<Map<String, String>> localTokens = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public RedisDistributedLockClient(StringRedisTemplate redisTemplate) {
        this.redis = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setScriptText(RELEASE_LUA);
        this.releaseScript.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {
        String token = UUID.randomUUID().toString();
        boolean ok = Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent(key, token, Duration.ofMillis(unit.toMillis(leaseTime))));
        String timeUnit = " " + unit;
        if (ok) {
            localTokens.get().put(key, token);
            if (log.isDebugEnabled()) {
                log.debug("lock acquired key={}, lease={}{}", key, leaseTime, timeUnit);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("lock busy key={}, lease={}{}", key, leaseTime, timeUnit);
            }
        }
        return ok;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit, SpinWaitConfig spin) {
        long deadline = System.nanoTime() + TimeUnit.NANOSECONDS.convert(waitTime, unit);
        int attempts = 0;
        int intervalMs = Math.max(0, spin == null ? 0 : (int) spin.unit().toMillis(spin.interval()));
        while (System.nanoTime() <= deadline) {
            if (tryLock(key, leaseTime, unit)) {
                return true;
            }
            if (spin != null) {
                attempts++;
                if (spin.maxAttempts() > 0 && attempts >= spin.maxAttempts()) {
                    break;
                }
                if (intervalMs > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(intervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (spin.strategy() == SpinWaitStrategy.LINEAR) {
                        intervalMs += spin.unit().toMillis(spin.interval());
                    } else if (spin.strategy() == SpinWaitStrategy.EXPONENTIAL) {
                        intervalMs = intervalMs * 2;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("spin wait key={}, attempt={}, nextIntervalMs={}", key, attempts, intervalMs);
                    }
                }
            } else {
                break;
            }
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        String token = localTokens.get().remove(key);
        if (token == null) {
            if (log.isWarnEnabled()) {
                log.warn("unlock skipped, no local token for key={}", key);
            }
            return;
        }
        redis.execute(releaseScript, Collections.singletonList(key), token);
        if (log.isDebugEnabled()) {
            log.debug("unlock executed for key={}", key);
        }
    }
}
