package com.github.chengtang.dlock;

import com.github.chengtang.dlock.annotation.Lock;
import com.github.chengtang.dlock.core.KeyResolver;
import com.github.chengtang.lockkey.LockKeyParam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyResolverTest {
    static class User {
        @LockKeyParam
        Long id;
        String name;
        public User(Long id, String name) { this.id = id; this.name = name; }
    }

    static class PlainUser {
        Long id;
        String name;
        public PlainUser(Long id, String name) { this.id = id; this.name = name; }
        public Long getId() { return id; }
        public String getName() { return name; }
    }

    static class Service {
        @Lock(prefix = "dl", delimiter = ":", timeUnit = TimeUnit.SECONDS, keys = {"#p0.id", "#p1"})
        public void onlySpEL(PlainUser user, Long orderId) {}

        @Lock(prefix = "dl", delimiter = ":", timeUnit = TimeUnit.SECONDS)
        public void onlyParam(@LockKeyParam("id") User user, @LockKeyParam Long orderId) {}

        @Lock(prefix = "dl", delimiter = ":", timeUnit = TimeUnit.SECONDS, keys = {"#p0.id", "#p1"})
        public void unionBoth(@LockKeyParam("id") User user, @LockKeyParam Long orderId) {}

        @Lock(prefix = "dl", delimiter = ":", timeUnit = TimeUnit.SECONDS)
        public void onlyField(User user, @LockKeyParam Long orderId) {}
    }

    @Test
    void buildKeyWithSpELOnly() throws Exception {
        Method m = Service.class.getDeclaredMethod("onlySpEL", PlainUser.class, Long.class);
        Lock lockAnn = m.getAnnotation(Lock.class);
        String key = KeyResolver.buildKey(lockAnn, m, new Object[]{new PlainUser(1L, "Alice"), 9L});
        assertEquals("dl:1:9", key);
    }

    @Test
    void buildKeyWithParamOnly() throws Exception {
        Method m = Service.class.getDeclaredMethod("onlyParam", User.class, Long.class);
        Lock lockAnn = m.getAnnotation(Lock.class);
        String key = KeyResolver.buildKey(lockAnn, m, new Object[]{new User(1L, "Alice"), 9L});
        assertEquals("dl:1:9", key);
    }

    @Test
    void buildKeyUnionBothParamFirstThenSpEL() throws Exception {
        Method m = Service.class.getDeclaredMethod("unionBoth", User.class, Long.class);
        Lock lockAnn = m.getAnnotation(Lock.class);
        String key = KeyResolver.buildKey(lockAnn, m, new Object[]{new User(1L, "Alice"), 9L});
        assertEquals("dl:1:9:1:9", key);
    }

    @Test
    void buildKeyWithFieldOnly() throws Exception {
        Method m = Service.class.getDeclaredMethod("onlyField", User.class, Long.class);
        Lock lockAnn = m.getAnnotation(Lock.class);
        String key = KeyResolver.buildKey(lockAnn, m, new Object[]{new User(1L, "Alice"), 9L});
        assertEquals("dl:1:9", key);
    }
}
