package com.github.chengtang.dlock.core;

import java.util.concurrent.TimeUnit;

public interface DistributedLockClient {
    boolean tryLock(String key, long leaseTime, TimeUnit unit);

    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit, SpinWaitConfig spin);

    void unlock(String key);
}

