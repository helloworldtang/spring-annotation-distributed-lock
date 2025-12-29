package com.github.chengtang.dlock.autoconfigure;

import com.github.chengtang.dlock.aop.DistributedLockAspect;
import com.github.chengtang.dlock.core.DistributedLockClient;
import com.github.chengtang.dlock.redis.RedisDistributedLockClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockClient distributedLockClient(StringRedisTemplate template) {
        return new RedisDistributedLockClient(template);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(DistributedLockClient client) {
        return new DistributedLockAspect(client);
    }
}

