package com.github.chengtang.dlock.annotation;

/**
 * 自旋等待的回退策略
 * FIXED：固定间隔；每次等待间隔不变
 * LINEAR：线性递增；每次在上次基础上 +interval
 * EXPONENTIAL：指数递增；每次在上次基础上 *2
 */
public enum SpinWaitStrategy {
    FIXED,
    LINEAR,
    EXPONENTIAL
}
