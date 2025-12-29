package com.github.chengtang.dlock.core;

import com.github.chengtang.dlock.annotation.SpinWaitStrategy;

import java.util.concurrent.TimeUnit;

/**
 * 自旋等待配置
 * interval：基础等待间隔
 * maxAttempts：最多尝试次数（>0生效）
 * strategy：回退策略（固定/线性/指数）
 * unit：间隔单位
 */
public class SpinWaitConfig {
    private final int interval;
    private final int maxAttempts;
    private final SpinWaitStrategy strategy;
    private final TimeUnit unit;

    public SpinWaitConfig(int interval, int maxAttempts, SpinWaitStrategy strategy, TimeUnit unit) {
        this.interval = interval;
        this.maxAttempts = maxAttempts;
        this.strategy = strategy;
        this.unit = unit;
    }

    public int interval() { return interval; }
    public int maxAttempts() { return maxAttempts; }
    public SpinWaitStrategy strategy() { return strategy; }
    public TimeUnit unit() { return unit; }
}
