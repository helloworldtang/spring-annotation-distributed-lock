package com.github.chengtang.dlock;

import com.github.chengtang.dlock.aop.DistributedLockAspect;
import com.github.chengtang.dlock.annotation.Lock;
import com.github.chengtang.lockkey.LockKeyParam;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AspectIntegrationTest {
    static class Service {
        private final AtomicInteger calls = new AtomicInteger();
        @Lock(prefix = "dl", delimiter = ":", expireTime = 1, waitTime = 1, timeUnit = TimeUnit.SECONDS, keys = {"#id"})
        public int doWork(@LockKeyParam Long id) {
            return calls.incrementAndGet();
        }
    }

    @Test
    void aspectLocksAroundInvocation() {
        Service target = new Service();
        DistributedLockAspect aspect = new DistributedLockAspect(new FakeDistributedLockClient());
        AspectJProxyFactory pf = new AspectJProxyFactory(target);
        pf.addAspect(aspect);
        Service proxy = pf.getProxy();
        int r1 = proxy.doWork(1L);
        int r2 = proxy.doWork(1L);
        assertEquals(2, r2);
        assertEquals(1, r1);
    }
}

