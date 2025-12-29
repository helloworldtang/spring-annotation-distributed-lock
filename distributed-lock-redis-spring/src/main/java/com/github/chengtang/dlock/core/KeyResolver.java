package com.github.chengtang.dlock.core;

import com.github.chengtang.dlock.annotation.Lock;
import com.github.chengtang.lockkey.LockKeyParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class KeyResolver {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private KeyResolver() {
    }

    public static String buildKey(Lock lockAnn, Method method, Object[] args) {
        List<String> parts = new ArrayList<>();
        if (lockAnn.prefix() != null && !lockAnn.prefix().trim().isEmpty()) {
            parts.add(lockAnn.prefix());
        }
        List<String> paramParts = extractKeyParams(method, args);

        String[] names = NAME_DISCOVERER.getParameterNames(method);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        java.util.Map<String, Object> varMap = new java.util.HashMap<>();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
                varMap.put(names[i], args[i]);
            }
        }
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("p" + i, args[i]);
            ctx.setVariable("a" + i, args[i]);
            ctx.setVariable("arg" + i, args[i]);
            varMap.put("p" + i, args[i]);
            varMap.put("a" + i, args[i]);
            varMap.put("arg" + i, args[i]);
        }
        ctx.setVariable("args", args);
        varMap.put("args", args);
        List<String> spelParts = new ArrayList<>();
        for (String keyExpr : lockAnn.keys()) {
            if (keyExpr != null && !keyExpr.trim().isEmpty()) {
                try {
                    Expression exp = PARSER.parseExpression(keyExpr);
                    Object val = exp.getValue(ctx);
                    if (val != null) {
                        spelParts.add(String.valueOf(val));
                    }
                } catch (Exception ignored) {
                }
                if (spelParts.isEmpty()) {
                    String expr = keyExpr.trim();
                    if (expr.startsWith("#")) {
                        expr = expr.substring(1);
                    }
                    String[] tokens = expr.split("\\.");
                    if (tokens.length > 0) {
                        Object base = varMap.get(tokens[0]);
                        if (base != null) {
                            Object v = base;
                            for (int i = 1; i < tokens.length; i++) {
                                v = readProperty(v, tokens[i]);
                                if (v == null) break;
                            }
                            if (v != null) {
                                spelParts.add(String.valueOf(v));
                            }
                        }
                    }
                }
            }
        }
        parts.addAll(paramParts);
        parts.addAll(spelParts);
        String built = String.join(lockAnn.delimiter(), parts);
        if (log.isDebugEnabled()) {
            log.debug("built lock key={}, parts={}", built, parts);
        }
        return built;
    }

    private static List<String> extractKeyParams(Method method, Object[] args) {
        List<String> parts = new ArrayList<>();
        java.lang.reflect.Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            java.lang.reflect.Parameter p = params[i];
            LockKeyParam ann = p.getAnnotation(LockKeyParam.class);
            if (ann != null) {
                Object argVal = args[i];
                String path = sanitize(ann.value());
                if (argVal == null) continue;
                if (path.isEmpty()) {
                    if (isSimple(argVal)) {
                        parts.add(String.valueOf(argVal));
                    } else {
                        parts.addAll(extractAnnotatedFields(argVal));
                    }
                } else {
                    Object v = readProperty(argVal, path);
                    if (v != null) {
                        parts.add(String.valueOf(v));
                    }
                }
            } else {
                parts.addAll(extractAnnotatedFields(args[i]));
            }
        }
        return parts;
    }

    private static List<String> extractAnnotatedFields(@Nullable Object bean) {
        List<String> parts = new ArrayList<>();
        if (bean == null || isSimple(bean)) return parts;
        Class<?> clazz = bean.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            LockKeyParam fieldAnn = f.getAnnotation(LockKeyParam.class);
            if (fieldAnn != null) {
                boolean acc = f.isAccessible();
                try {
                    if (!acc) {
                        f.setAccessible(true);
                    }
                    Object v = f.get(bean);
                    String path = sanitize(fieldAnn.value());
                    Object val = v;
                    if (!path.isEmpty() && v != null) {
                        val = readProperty(v, path);
                    }
                    if (val != null) {
                        parts.add(String.valueOf(val));
                    }
                } catch (IllegalAccessException ignored) {
                } finally {
                    if (!acc) {
                        f.setAccessible(false);
                    }
                }
            }
        }
        return parts;
    }

    private static boolean isSimple(Object o) {
        Class<?> c = o.getClass();
        return c.isPrimitive()
                || Number.class.isAssignableFrom(c)
                || CharSequence.class.isAssignableFrom(c)
                || Boolean.class.isAssignableFrom(c)
                || Character.class.isAssignableFrom(c);
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.trim();
    }

    private static Object readProperty(Object root, String path) {
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext(root);
            Expression exp = PARSER.parseExpression(path);
            Object val = exp.getValue(ctx);
            if (val != null) {
                return val;
            }
        } catch (Exception ignored) {
        }
        try {
            String[] tokens = path.split("\\.");
            Object current = root;
            for (String token : tokens) {
                if (current == null) {
                    return null;
                }
                Class<?> c = current.getClass();
                Field f;
                try {
                    f = c.getDeclaredField(token);
                } catch (NoSuchFieldException nf) {
                    return null;
                }
                boolean acc = f.isAccessible();
                if (!acc) {
                    f.setAccessible(true);
                }
                current = f.get(current);
                if (!acc) {
                    f.setAccessible(false);
                }
            }
            return current;
        } catch (Exception e) {
            log.warn("readProperty error : path:{} msg {} ", path, e.getMessage());
            return null;
        }
    }
}
