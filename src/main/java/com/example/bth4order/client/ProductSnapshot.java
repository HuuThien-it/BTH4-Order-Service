package com.example.bth4order.client;

import java.math.BigDecimal;

// The JSON response contract exposed by BTH3: GET /products/{id}
public record ProductSnapshot(Long id, String name, BigDecimal price, Integer quantity) { }
