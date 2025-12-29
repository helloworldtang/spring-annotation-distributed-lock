package com.github.chengtang.dlock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpinWaitTimeParam {
    int interval() default 0;

    int maxAttempts() default 0;

    SpinWaitStrategy strategy() default SpinWaitStrategy.FIXED;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}

