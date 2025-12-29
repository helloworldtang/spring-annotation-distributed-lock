package com.github.chengtang.dlock.aop;

import com.github.chengtang.dlock.annotation.Lock;
import com.github.chengtang.dlock.annotation.SpinWaitTimeParam;
import com.github.chengtang.dlock.core.DistributedLockClient;
import com.github.chengtang.dlock.core.KeyResolver;
import com.github.chengtang.dlock.core.SpinWaitConfig;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
@Slf4j
public class DistributedLockAspect {
    private final DistributedLockClient lockClient;

    public DistributedLockAspect(DistributedLockClient lockClient) {
        this.lockClient = lockClient;
    }

    @Around("@annotation(lockAnn)")
    public Object around(ProceedingJoinPoint pjp, Lock lockAnn) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = KeyResolver.buildKey(lockAnn, method, pjp.getArgs());
        boolean acquired;
        SpinWaitTimeParam spin = lockAnn.spinWaitTimeParam();
        SpinWaitConfig spinCfg = new SpinWaitConfig(spin.interval(), spin.maxAttempts(), spin.strategy(), spin.timeUnit());
        if (log.isDebugEnabled()) {
            log.debug("try acquire key={}, wait={} {}, expire={} {}", key, lockAnn.waitTime(), lockAnn.timeUnit(), lockAnn.expireTime(), lockAnn.timeUnit());
        }
        if (lockAnn.waitTime() > 0) {
            acquired = lockClient.tryLock(key, lockAnn.waitTime(), lockAnn.expireTime(), lockAnn.timeUnit(), spinCfg);
        } else {
            acquired = lockClient.tryLock(key, lockAnn.expireTime(), lockAnn.timeUnit());
        }
        if (!acquired) {
            if (log.isWarnEnabled()) {
                log.warn("lock acquire failed key={}", key);
            }
            throw new IllegalStateException("already lock.Failed to acquire distributed lock for key=" + key);
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("lock acquired, proceed key={}", key);
            }
            return pjp.proceed();
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("release lock key={}", key);
            }
            lockClient.unlock(key);
        }
    }
}
