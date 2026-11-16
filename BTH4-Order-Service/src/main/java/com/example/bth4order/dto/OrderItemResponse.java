package com.example.bth4order.dto;

import com.example.bth4order.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long id, Long productId, String productName,
        BigDecimal unitPrice, Integer quantity, BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProductId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
    }
}
