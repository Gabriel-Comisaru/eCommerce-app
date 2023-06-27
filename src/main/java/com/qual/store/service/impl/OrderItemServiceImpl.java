package com.qual.store.service.impl;

import com.qual.store.exceptions.OrderItemNotFoundException;
import com.qual.store.exceptions.ProductNotFoundException;
import com.qual.store.logger.Log;
import com.qual.store.model.OrderItem;
import com.qual.store.model.Product;
import com.qual.store.repository.OrderItemRepository;
import com.qual.store.repository.ProductRepository;
import com.qual.store.service.OrderItemService;
import com.qual.store.utils.validators.Validator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private Validator<OrderItem> validator;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Log
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    @Override
    @Log
    public void deleteOrderItemById(Long id) {
        orderItemRepository.deleteById(id);
    }

    @Override
    @Log
    public void modifyQuantity(Long idOrderItem, Integer newQuantity) {
        OrderItem orderItem1 = orderItemRepository.findById(idOrderItem).
                orElseThrow(() -> new OrderItemNotFoundException(String.format("No order item found with id %s", idOrderItem)));
        if (newQuantity >= 1) {
            orderItem1.setQuantity(newQuantity);
            orderItemRepository.save(orderItem1);
        } else {
            orderItemRepository.delete(orderItem1);
        }
    }

    @Transactional
    @Override
    @Log
    public OrderItem addOrderItem(Long id, OrderItem orderItem) {
        validator.validate(orderItem);
        // verify if the item is already in the cart
        Product product = productRepository.findById(id).
                orElseThrow(() -> new ProductNotFoundException(String.format("No product with is found:%s", id)));
        orderItem.setProduct(product);
        product.addOrderItem(orderItem);
        productRepository.save(product);
        return orderItem;
    }

    @Override
    @Log
    public Optional<OrderItem> findOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }

    @Override
    @Log
    public double priceOfOrderItem(Long id) {
        OrderItem orderItem = orderItemRepository.findById(id).
                orElseThrow(() -> new OrderItemNotFoundException(String.format("No order item found with id %s", id)));
        return orderItem.getQuantity() * orderItem.getProduct().getPrice();
    }
}