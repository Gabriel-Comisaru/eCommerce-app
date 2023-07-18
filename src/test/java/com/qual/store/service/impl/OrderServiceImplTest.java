package com.qual.store.service.impl;


import com.qual.store.converter.OrderConverter;
import com.qual.store.dto.OrderDto;
import com.qual.store.exceptions.InvalidOrderStatusException;
import com.qual.store.exceptions.OrderItemNotFoundException;
import com.qual.store.exceptions.OrderNotFoundException;
import com.qual.store.exceptions.UpdateOrderStatusException;
import com.qual.store.model.AppUser;
import com.qual.store.model.Order;
import com.qual.store.model.OrderItem;
import com.qual.store.model.Product;
import com.qual.store.model.enums.OrderStatus;
import com.qual.store.model.enums.RoleName;
import com.qual.store.repository.AppUserRepository;
import com.qual.store.repository.OrderItemRepository;
import com.qual.store.repository.OrderRepository;
import com.qual.store.service.OrderItemService;
import com.qual.store.utils.validators.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrderConverter orderConverter;

    @Mock
    private Validator<Order> validator;

    @Mock
    private OrderItemService orderItemService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getAllOrdersTest() {
        // given
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.ACTIVE);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(1);
        orderItem.setOrder(order);

        order.getOrderItems().add(orderItem);

        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setFirstName("John");
        appUser.setLastName("Doe");
        appUser.setEmail("johndoe@yahoo.com");

        appUser.setOrders(new HashSet<>());
        appUser.getOrders().add(order);

        order.setUser(appUser);

        List<Order> expectedResult = new ArrayList<>();
        expectedResult.add(order);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(expectedResult);
        List<Order> actualResult = orderService.getAllOrders();

        // then
        assertEquals(expectedResult, actualResult);
        verify(orderRepository, times(1)).findAllWithOrderItems();
    }

    @Test
    public void addToOrderTest() {
        // given
        OrderItem orderItem = OrderItem.builder().build();
        orderItem.setId(1L);

        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.addOrderItem(orderItem);

        Authentication authentication = new UsernamePasswordAuthenticationToken(new AppUser(), new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        //when
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(new AppUser());
        Order savedOrder = orderService.addToOrder(orderItem.getId());

        //then
        assertEquals(order, savedOrder);
        verify(validator, times(1)).validate(order);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    public void addToOrderThrowsOrderItemNotFoundExceptionTest() {
        // given
        Long orderItemId = 1L;

        Authentication authentication = new UsernamePasswordAuthenticationToken(new AppUser(), new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        // then
        assertThrows(OrderItemNotFoundException.class,
                () -> orderService.addToOrder(orderItemId)
        );
        verify(validator, times(1)).validate(any());
        verify(orderRepository, times(0)).save(any());
    }

    @Test
    public void deleteOrderByIdTest() {
        // given
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        OrderItem orderItem = OrderItem.builder().build();
        order.setId(1L);
        orderItem.setId(1L);
        orderItem.setOrder(order);
        order.addOrderItem(orderItem);

        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem);

        Authentication authentication = new UsernamePasswordAuthenticationToken(new AppUser(), new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.saveAll(order.getOrderItems())).thenReturn(new ArrayList<>());
        orderService.deleteOrderById(order.getId());

        // then
        verify(orderRepository, times(1)).findById(order.getId());
        verify(orderItemRepository, times(1)).saveAll(orderItems);
        verify(orderRepository, times(1)).deleteById(order.getId());

    }

    @Test
    void deleteOrderByIdThrowsOrderNotFoundExceptionTest() {
        // given
        Long orderId = 1L;

        // when
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // then
        assertThrows(OrderNotFoundException.class,
                () -> orderService.deleteOrderById(orderId)
        );
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderItemRepository, times(0)).saveAll(any());
        verify(orderRepository, times(0)).deleteById(any());
    }

    @Test
    void findOrderByIdTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        // when
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Order actualResult = orderService.findOrderById(orderId);

        // then
        assertNotNull(actualResult);
        assertEquals(orderId, actualResult.getId());
        assertEquals(order.getStatus(), actualResult.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void findOrderByIdThrowsOrderNotFoundException() {
        // given
        Long orderId = 1L;

        // when
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // then
        assertThrows(OrderNotFoundException.class,
                () -> orderService.findOrderById(orderId)
        );
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void updateOrderStatusAsAdminTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        Optional<Order> optionalOrder = Optional.of(order);
        AppUser appUser = new AppUser();
        appUser.setUsername("username");
        appUser.setRole(RoleName.ADMIN);
        order.setUser(appUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(List.of(order));
        when(orderRepository.findById(orderId)).thenReturn(optionalOrder);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);
        Order actualResult = orderService.updateOrderStatus(orderId, "DELIVERED");

        // then
        assertNotNull(actualResult);
        assertEquals(orderId, actualResult.getId());
        assertEquals(OrderStatus.DELIVERED, actualResult.getStatus());
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
    }

    @Test
    void updateOrderStatusAsUserTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        Optional<Order> optionalOrder = Optional.of(order);
        AppUser appUser = new AppUser();
        appUser.setUsername("username");
        appUser.setRole(RoleName.USER);
        order.setUser(appUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(List.of(order));
        when(orderRepository.findById(orderId)).thenReturn(optionalOrder);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);
        Order actualResult = orderService.updateOrderStatus(orderId, "CHECKOUT");

        // then
        assertNotNull(actualResult);
        assertEquals(orderId, actualResult.getId());
        assertEquals(OrderStatus.CHECKOUT, actualResult.getStatus());
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
    }

    @Test
    void updateOrderStatusThrowsOrderNotFoundExceptionTest() {
        // given
        Long orderId = 1L;

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(new ArrayList<>());

        // then
        assertThrows(OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(orderId, "DELIVERED")
        );
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(0)).findUserByUsername(anyString());
    }

    @Test
    void updateOrderStatusAsAdminThrowsInvalidOrderStatusExceptionTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        Optional<Order> optionalOrder = Optional.of(order);
        AppUser appUser = new AppUser();
        appUser.setUsername("username");
        appUser.setRole(RoleName.ADMIN);
        order.setUser(appUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(List.of(order));
        when(orderRepository.findById(orderId)).thenReturn(optionalOrder);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);

        // then
        assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, "FAKE_STATUS")
        );
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
    }

    @Test
    void updateOrderStatusAsUserThrowsInvalidOrderStatusExceptionTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        Optional<Order> optionalOrder = Optional.of(order);
        AppUser appUser = new AppUser();
        appUser.setUsername("username");
        appUser.setRole(RoleName.USER);
        order.setUser(appUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(List.of(order));
        when(orderRepository.findById(orderId)).thenReturn(optionalOrder);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);

        // then
        assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, "DELIVERED")
        );
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
    }

    @Test
    void updateOrderStatusAsUserThrowsUpdateOrderStatusExceptionTest() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .status(OrderStatus.PLACED)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(orderId);

        Optional<Order> optionalOrder = Optional.of(order);
        AppUser appUser = new AppUser();
        appUser.setUsername("username");
        appUser.setRole(RoleName.USER);
        order.setUser(appUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderRepository.findAllWithOrderItems()).thenReturn(List.of(order));
        when(orderRepository.findById(orderId)).thenReturn(optionalOrder);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);

        // then
        assertThrows(UpdateOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, "CHECKOUT")
        );
        verify(orderRepository, times(1)).findAllWithOrderItems();
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
    }

    @Test
    void getAllOrdersByUserTest() {
        // given
        Long userId = 1L;
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setUsername("username");
        appUser.setRole(RoleName.USER);

        Order order = Order.builder()
                .status(OrderStatus.ACTIVE)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setUser(appUser);

        OrderDto orderDto = new OrderDto();
        orderDto.setStatus("ACTIVE");
        orderDto.setDeliveryPrice(200.0);
        orderDto.setOrderItems(new ArrayList<>());
        orderDto.setUserId(userId);

        List<Order> orders = List.of(order);


        Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, new Object());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        when(orderService.getAllOrders()).thenReturn(orders);
        when(appUserRepository.findUserByUsername(anyString())).thenReturn(appUser);
        when(orderConverter.convertModelToDto(any(Order.class))).thenReturn(orderDto);
        when(orderConverter.convertDtoToModel(any(OrderDto.class))).thenReturn(order);
        List<Order> actualResult = orderService.getAllOrdersByUser();

        // then
        assertNotNull(actualResult);
        assertEquals(1, actualResult.size());
        assertEquals(order, actualResult.get(0));
        verify(appUserRepository, times(1)).findUserByUsername(anyString());
        verify(orderConverter, times(1)).convertModelToDto(any(Order.class));
        verify(orderConverter, times(1)).convertDtoToModel(any(OrderDto.class));
    }

    @Test
    void getProductsQuantityTest() {
        // given
        Product product = Product.builder()
                .name("product")
                .price(100.0)
                .build();
        product.setId(1L);

        OrderItem orderItem = OrderItem.builder()
                .quantity(2)
                .product(product)
                .build();
        orderItem.setId(1L);

        Order order = Order.builder()
                .status(OrderStatus.PLACED)
                .deliveryPrice(200.0)
                .orderItems(new HashSet<>())
                .build();
        order.setId(1L);
        order.getOrderItems().add(orderItem);

        // when
        when(orderService.getAllOrders()).thenReturn(List.of(order));
        Map<Long, Integer> actualResult = orderService.getProductsQuantity();

        // then
        assertNotNull(actualResult);
        assertEquals(1, actualResult.size());
        assertEquals(2, actualResult.get(1L));

    }

    @AfterEach
    public void closeService() throws Exception {
        closeable.close();
    }
}