package com.github.chengtang.sample.service;

import com.github.chengtang.dlock.annotation.Lock;
import com.github.chengtang.dlock.annotation.SpinWaitStrategy;
import com.github.chengtang.dlock.annotation.SpinWaitTimeParam;
import com.github.chengtang.lockkey.LockKeyParam;
import com.github.chengtang.sample.dto.OrderRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {
    private final AtomicInteger calls = new AtomicInteger();

    @Lock(prefix = "dl", delimiter = ":", expireTime = 5, waitTime = 0, timeUnit = TimeUnit.SECONDS)
    public int place(OrderRequest req, @LockKeyParam Long orderId) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return calls.incrementAndGet();
    }

    @Lock(prefix = "dl", delimiter = ":", expireTime = 5, waitTime = 2, timeUnit = TimeUnit.SECONDS)
    public int placeWait(OrderRequest req, @LockKeyParam Long orderId) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return calls.incrementAndGet();
    }

    @Lock(prefix = "dl", delimiter = ":", expireTime = 5, waitTime = 3, timeUnit = TimeUnit.SECONDS,
            spinWaitTimeParam = @SpinWaitTimeParam(interval = 100, maxAttempts = 0, strategy = SpinWaitStrategy.LINEAR))
    public int placeWaitLinear(OrderRequest req, @LockKeyParam Long orderId) {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return calls.incrementAndGet();
    }
}
