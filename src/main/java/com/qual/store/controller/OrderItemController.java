package com.qual.store.controller;

import com.qual.store.converter.OrderItemConverter;
import com.qual.store.dto.OrderItemDto;
import com.qual.store.logger.Log;
import com.qual.store.model.BaseEntity;
import com.qual.store.model.OrderItem;
import com.qual.store.model.Product;
import com.qual.store.service.OrderItemService;
import com.qual.store.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/orderItems")
public class OrderItemController {
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private OrderItemConverter orderItemConverter;
    @Autowired
    private ProductService productService;

    @GetMapping
    @Log
    public List<OrderItemDto> getAllOrderItems() {
        return orderItemService.getAllOrderItems().stream()
                .map(orderItem -> orderItemConverter.convertModelToDto(orderItem))
                .collect(Collectors.toList());
    }

    @PostMapping(value = "/{productId}")
    @Log
    public OrderItemDto addOrderItem(@PathVariable("productId") Long productId, @RequestBody OrderItem orderItem) {
        OrderItem savedOrderItem = orderItemService.addOrderItem(productId, orderItem);
        System.out.println(savedOrderItem);
        return orderItemConverter.convertModelToDto(savedOrderItem);
    }

    @DeleteMapping(value = "/{id}")
    @Log
    public ResponseEntity<?> deleteOrderItemById(@PathVariable("id") Long id) {
         orderItemService.findOrderItemById(id).orElseThrow(() -> new RuntimeException("OrderItem not found"));
        System.out.println(orderItemService.findOrderItemById(id));
         orderItemService.deleteOrderItemById(id);
         return ResponseEntity.status(HttpStatus.OK)
                 .body("OrderItem deleted");
    }

    @PutMapping(value = "/{id}/quantity")
    @Log
    public ResponseEntity<?> decreaseQuantity(@PathVariable("id") Long id, @RequestParam Integer quantity) {
        OrderItem orderItem = orderItemService.findOrderItemById(id).orElseThrow(() -> new RuntimeException("OrderItem not found"));
        if (orderItem != null) {
            orderItemService.modifyQuantity(id, quantity);
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Quantity modified");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("OrderItem not found");
    }
}
