package com.example.bth4order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotEmpty List<@Valid OrderItemRequest> items
) { }
