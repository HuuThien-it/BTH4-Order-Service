package com.example.bth4order.dto;

import com.example.bth4order.entity.OrderStatus;
import com.example.bth4order.entity.PurchaseOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id, String customerName, OrderStatus status,
        BigDecimal totalAmount, List<OrderItemResponse> items,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(order.getId(), order.getCustomerName(), order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
