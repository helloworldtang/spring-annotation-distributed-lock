package com.github.chengtang.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6379"
})
class OrderServiceIntegrationTest {
    @LocalServerPort
    int port;

    @Test
    void concurrentLockShouldFastFailSecond() throws Exception {
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:" + port + "/orders/place";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> body = new HttpEntity<>("{\"userId\":1,\"orderId\":9}", headers);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger s1 = new AtomicInteger(0);
        AtomicInteger s2 = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            latch.countDown();
            try {
                org.springframework.http.ResponseEntity<Integer> resp =
                        rt.postForEntity(url, body, Integer.class);
                s1.set(resp.getStatusCode().value());
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                s1.set(e.getStatusCode().value());
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                try {
                    org.springframework.http.ResponseEntity<String> resp =
                            rt.postForEntity(url, body, String.class);
                    s2.set(resp.getStatusCode().value());
                } catch (org.springframework.web.client.HttpServerErrorException e) {
                    s2.set(e.getStatusCode().value());
                }
            } catch (InterruptedException ignored) {
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(700, s1.get() + s2.get());
    }

    @Test
    void concurrentLockShouldWaitThenSucceedSecond() throws Exception {
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:" + port + "/orders/place-wait";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> body = new HttpEntity<>("{\"userId\":1,\"orderId\":9}", headers);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger s1 = new AtomicInteger(0);
        AtomicInteger s2 = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            latch.countDown();
            try {
                org.springframework.http.ResponseEntity<Integer> resp =
                        rt.postForEntity(url, body, Integer.class);
                s1.set(resp.getStatusCode().value());
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                s1.set(e.getStatusCode().value());
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                try {
                    org.springframework.http.ResponseEntity<String> resp =
                            rt.postForEntity(url, body, String.class);
                    s2.set(resp.getStatusCode().value());
                } catch (org.springframework.web.client.HttpServerErrorException e) {
                    s2.set(e.getStatusCode().value());
                }
            } catch (InterruptedException ignored) {
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(400, s1.get() + s2.get());
    }

    @Test
    void concurrentLockShouldWaitLinearWithinWindow() throws Exception {
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:" + port + "/orders/place-wait-linear";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> body = new HttpEntity<>("{\"userId\":1,\"orderId\":9}", headers);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger s1 = new AtomicInteger(0);
        AtomicInteger s2 = new AtomicInteger(0);
        AtomicLong t2cost = new AtomicLong(0);
        Thread t1 = new Thread(() -> {
            latch.countDown();
            try {
                org.springframework.http.ResponseEntity<Integer> resp =
                        rt.postForEntity(url, body, Integer.class);
                s1.set(resp.getStatusCode().value());
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                s1.set(e.getStatusCode().value());
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                long start = System.currentTimeMillis();
                try {
                    org.springframework.http.ResponseEntity<String> resp =
                            rt.postForEntity(url, body, String.class);
                    s2.set(resp.getStatusCode().value());
                } catch (org.springframework.web.client.HttpServerErrorException e) {
                    s2.set(e.getStatusCode().value());
                } finally {
                    t2cost.set(System.currentTimeMillis() - start);
                }
            } catch (InterruptedException ignored) {
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(400, s1.get() + s2.get());
        // 线性回退+服务耗时约800ms，第二次耗时应在700ms~3000ms之间
        long cost = t2cost.get();
        org.junit.jupiter.api.Assertions.assertTrue(cost >= 700 && cost <= 3000, "t2 cost=" + cost);
    }
}
