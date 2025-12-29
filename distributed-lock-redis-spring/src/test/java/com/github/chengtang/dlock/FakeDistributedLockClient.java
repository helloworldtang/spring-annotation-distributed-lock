package com.github.chengtang.dlock;

import com.github.chengtang.dlock.core.DistributedLockClient;
import com.github.chengtang.dlock.core.SpinWaitConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FakeDistributedLockClient implements DistributedLockClient {
    private final Map<String, Long> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long exp = now + unit.toMillis(leaseTime);
        return locks.putIfAbsent(key, exp) == null;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit, SpinWaitConfig spin) {
        long deadline = System.nanoTime() + TimeUnit.NANOSECONDS.convert(waitTime, unit);
        while (System.nanoTime() <= deadline) {
            if (tryLock(key, leaseTime, unit)) return true;
            try { Thread.sleep(1); } catch (InterruptedException ignored) { }
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        locks.remove(key);
    }
}

