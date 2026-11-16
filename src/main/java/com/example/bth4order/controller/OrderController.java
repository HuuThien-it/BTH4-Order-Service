package com.example.bth4order.controller;

import com.example.bth4order.dto.OrderRequest;
import com.example.bth4order.dto.OrderResponse;
import com.example.bth4order.dto.UpdateStatusRequest;
import com.example.bth4order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/orders", "/api/orders"})
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/orders/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
