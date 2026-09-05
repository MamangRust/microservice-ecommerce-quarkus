package com.sanedge.order_item.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order_item.domain.requests.CreateOrderItemRequest;
import com.sanedge.order_item.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.entity.OrderItem;
import com.sanedge.order_item.repository.OrderItemRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private TracingMetrics tracingMetrics;

    private OrderItemCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        service = new OrderItemCommandServiceImpl(orderItemRepository, tracingMetrics);
    }

    @Test
    void create_shouldSucceed_whenValidRequest() {
        CreateOrderItemRequest request = new CreateOrderItemRequest();
        request.setOrderId(1);
        request.setProductId(100);
        request.setQuantity(2);
        request.setPrice(5000);

        OrderItem savedItem = createOrderItem(1L, 1, 100, 2, 5000);

        when(orderItemRepository.persist(any(OrderItem.class))).thenReturn(Uni.createFrom().item(savedItem));

        ApiResponse<OrderItemResponse> result = service.create(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item created successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getId()).isEqualTo(1L);
        assertThat(result.data().getOrderId()).isEqualTo(1);
        assertThat(result.data().getProductId()).isEqualTo(100);
        assertThat(result.data().getQuantity()).isEqualTo(2);
        assertThat(result.data().getPrice()).isEqualTo(5000);

        verify(orderItemRepository).persist(any(OrderItem.class));
    }

    @Test
    void create_shouldFail_whenRepositoryFails() {
        CreateOrderItemRequest request = new CreateOrderItemRequest();
        request.setOrderId(1);
        request.setProductId(100);
        request.setQuantity(2);
        request.setPrice(5000);

        when(orderItemRepository.persist(any(OrderItem.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }

    @Test
    void update_shouldSucceed_whenOrderItemExists() {
        UpdateOrderItemRequest request = new UpdateOrderItemRequest();
        request.setOrderItemId(1);
        request.setQuantity(5);
        request.setPrice(7500);

        OrderItem existingItem = createOrderItem(1L, 1, 100, 2, 5000);
        OrderItem updatedItem = createOrderItem(1L, 1, 100, 5, 7500);

        when(orderItemRepository.findById(1L)).thenReturn(Uni.createFrom().item(existingItem));
        when(orderItemRepository.persist(any(OrderItem.class))).thenReturn(Uni.createFrom().item(updatedItem));

        ApiResponse<OrderItemResponse> result = service.update(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item updated successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getQuantity()).isEqualTo(5);
        assertThat(result.data().getPrice()).isEqualTo(7500);
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenOrderItemNotFound() {
        UpdateOrderItemRequest request = new UpdateOrderItemRequest();
        request.setOrderItemId(999);
        request.setQuantity(5);
        request.setPrice(7500);

        when(orderItemRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order item not found");
    }

    @Test
    void trash_shouldSucceed_whenOrderItemExists() {
        OrderItem trashedItem = createOrderItem(1L, 1, 100, 2, 5000);
        trashedItem.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(orderItemRepository.trash(1L)).thenReturn(Uni.createFrom().item(trashedItem));

        ApiResponse<OrderItemResponseDeleteAt> result = service.trash(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item trashed successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getDeletedAt()).isNotNull();
    }

    @Test
    void trash_shouldThrowResourceNotFoundException_whenOrderItemNotFound() {
        when(orderItemRepository.trash(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> service.trash(999L).await().indefinitely())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order item not found");
    }

    @Test
    void restore_shouldSucceed_whenOrderItemInTrash() {
        OrderItem restoredItem = createOrderItem(1L, 1, 100, 2, 5000);

        when(orderItemRepository.restore(1L)).thenReturn(Uni.createFrom().item(restoredItem));

        ApiResponse<OrderItemResponseDeleteAt> result = service.restore(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item restored successfully");
    }

    @Test
    void restore_shouldThrowResourceNotFoundException_whenOrderItemNotInTrash() {
        when(orderItemRepository.restore(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> service.restore(999L).await().indefinitely())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order item not found in trash");
    }

    @Test
    void deletePermanent_shouldSucceed_whenOrderItemInTrash() {
        OrderItem deletedItem = createOrderItem(1L, 1, 100, 2, 5000);

        when(orderItemRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(deletedItem));

        ApiResponse<Void> result = service.deletePermanent(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item permanently deleted");
    }

    @Test
    void deletePermanent_shouldThrowInvalidRequestException_whenOrderItemNotInTrash() {
        when(orderItemRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> service.deletePermanent(999L).await().indefinitely())
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Order item not found or must be trashed before permanent deletion");
    }

    @Test
    void calculateTotalPrice_shouldReturnCorrectTotal() {
        OrderItem item1 = createOrderItem(1L, 1, 100, 2, 1000);
        OrderItem item2 = createOrderItem(2L, 1, 101, 3, 500);
        OrderItem item3 = createOrderItem(3L, 1, 102, 1, 3000);

        when(orderItemRepository.findOrderItemByOrder(1L))
                .thenReturn(Uni.createFrom().item(java.util.List.of(item1, item2, item3)));

        ApiResponse<Integer> result = service.calculateTotalPrice(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Total price calculated");
        assertThat(result.data()).isEqualTo(6500);
    }

    @Test
    void calculateTotalPrice_shouldReturnZero_whenNoItems() {
        when(orderItemRepository.findOrderItemByOrder(1L))
                .thenReturn(Uni.createFrom().item(java.util.List.of()));

        ApiResponse<Integer> result = service.calculateTotalPrice(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isEqualTo(0);
    }

    private OrderItem createOrderItem(Long id, Integer orderId, Integer productId, Integer quantity, Integer price) {
        OrderItem item = new OrderItem();
        item.id = id;
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(price);
        item.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        item.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return item;
    }

    /**
     * Finds the Supplier argument in the invocation regardless of whether it was
     * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
     * (arg index 3), then invokes it and returns the resulting Uni. This lets
     * a single Answer<?> body serve both traceAndMeasure overloads.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}
