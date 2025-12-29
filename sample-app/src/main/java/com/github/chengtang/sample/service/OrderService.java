package com.github.chengtang.sample.service;

import com.github.chengtang.dlock.annotation.Lock;
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
}
