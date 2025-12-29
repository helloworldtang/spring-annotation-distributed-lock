package com.github.chengtang.dlock.annotation;

import org.springframework.core.annotation.Order;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Order(1)
public @interface Lock {
    String prefix() default "dl";

    String delimiter() default ":";

    int expireTime() default 10;

    int waitTime() default 3;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    SpinWaitTimeParam spinWaitTimeParam() default @SpinWaitTimeParam();

    String[] keys() default {};
}

