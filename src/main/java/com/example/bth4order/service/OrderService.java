package com.example.bth4order.service;

import com.example.bth4order.client.ProductClient;
import com.example.bth4order.client.ProductSnapshot;
import com.example.bth4order.dto.*;
import com.example.bth4order.entity.*;
import com.example.bth4order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final ProductClient productClient;

    public OrderService(OrderRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll().stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrThrow(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        List<OrderItem> items = resolveAndValidateItems(request.items());
        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerName(request.customerName().trim());
        order.setStatus(OrderStatus.PENDING);
        order.replaceItems(items);
        order.setTotalAmount(sumTotals(items));
        return OrderResponse.from(repository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        PurchaseOrder order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING orders can be edited");
        }
        List<OrderItem> items = resolveAndValidateItems(request.items());
        order.setCustomerName(request.customerName().trim());
        order.replaceItems(items);
        order.setTotalAmount(sumTotals(items));
        return OrderResponse.from(repository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateStatusRequest request) {
        PurchaseOrder order = getOrThrow(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        if (current == target) {
            return OrderResponse.from(order);
        }
        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.COMPLETED || target == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid status transition: " + current + " -> " + target);
        }
        order.setStatus(target);
        return OrderResponse.from(repository.saveAndFlush(order));
    }

    @Transactional
    public void delete(Long id) {
        PurchaseOrder order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING or CANCELLED orders can be deleted");
        }
        repository.delete(order);
    }

    private PurchaseOrder getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    private List<OrderItem> resolveAndValidateItems(List<OrderItemRequest> requests) {
        // Merge duplicate product IDs before stock checks to avoid bypassing quantity limits.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : requests) {
            if (item == null || item.productId() == null || item.productId() <= 0
                    || item.quantity() == null || item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Items must contain positive productId and quantity");
            }
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is too large");
            }
        }
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductSnapshot product = productClient.findProduct(entry.getKey());
            if (product.quantity() < entry.getValue()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Not enough stock for product " + entry.getKey()
                                + ": available=" + product.quantity() + ", requested=" + entry.getValue());
            }
            OrderItem item = new OrderItem();
            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setUnitPrice(product.price());
            item.setQuantity(entry.getValue());
            item.setSubtotal(product.price().multiply(BigDecimal.valueOf(entry.getValue())));
            items.add(item);
        }
        return items;
    }

    private BigDecimal sumTotals(List<OrderItem> items) {
        return items.stream().map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
