package com.example.bth4order.dto;

import com.example.bth4order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) { }
