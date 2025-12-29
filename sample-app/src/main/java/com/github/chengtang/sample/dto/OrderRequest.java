package com.github.chengtang.sample.dto;

import com.github.chengtang.lockkey.LockKeyParam;

public class OrderRequest {
    @LockKeyParam
    private Long userId;
    @LockKeyParam
    private Long orderId;

    public OrderRequest() {}
    public OrderRequest(Long userId, Long orderId) {
        this.userId = userId;
        this.orderId = orderId;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
