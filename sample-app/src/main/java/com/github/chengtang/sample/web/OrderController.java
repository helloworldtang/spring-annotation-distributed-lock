package com.github.chengtang.sample.web;

import com.github.chengtang.sample.dto.OrderRequest;
import com.github.chengtang.sample.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) {
        this.service = service;
    }
    @PostMapping("/orders/place")
    public int place(@RequestBody OrderRequest req) {
        return service.place(req, req.getOrderId());
    }

    @PostMapping("/orders/place-wait")
    public int placeWait(@RequestBody OrderRequest req) {
        return service.placeWait(req, req.getOrderId());
    }

    @PostMapping("/orders/place-wait-linear")
    public int placeWaitLinear(@RequestBody OrderRequest req) {
        return service.placeWaitLinear(req, req.getOrderId());
    }
}
