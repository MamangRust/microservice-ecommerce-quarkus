package com.sanedge.order_item.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.repository.OrderItemRepository;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderItemQueryServiceImpl implements OrderItemQueryService {
    private static final Logger logger = LoggerFactory.getLogger(OrderItemQueryServiceImpl.class);

    private final OrderItemRepository orderItemRepository;
    private final TracingMetrics tracingMetrics;

    @Inject
    public OrderItemQueryServiceImpl(OrderItemRepository orderItemRepository, TracingMetrics tracingMetrics) {
        this.orderItemRepository = orderItemRepository;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Uni<ApiResponse<List<OrderItemResponse>>> findAll(FindAllOrderItemRequest request) {
        logger.info("Finding all order items with request: {}", request);

        return tracingMetrics.traceAndMeasure("findAllOrderItems", "find_all_order_items",
                () -> orderItemRepository.findOrderItems(request)
                        .map(paged -> {
                            List<OrderItemResponse> responses = paged.getData().stream()
                                    .map(OrderItemResponse::from)
                                    .collect(Collectors.toList());
                            logger.info("Successfully retrieved {} order items", responses.size());
                            return ApiResponse.success("Order items retrieved successfully", responses);
                        })
                        .onFailure().invoke(e -> logger.error("Error finding all order items", e)));
    }

    @Override
    public Uni<ApiResponse<List<OrderItemResponseDeleteAt>>> findActive(FindAllOrderItemRequest request) {
        logger.info("Finding active order items with request: {}", request);

        return tracingMetrics.traceAndMeasure("findActiveOrderItems", "find_active_order_items",
                () -> orderItemRepository.findActiveOrderItems(request)
                        .map(paged -> {
                            List<OrderItemResponseDeleteAt> responses = paged.getData().stream()
                                    .map(OrderItemResponseDeleteAt::from)
                                    .collect(Collectors.toList());
                            logger.info("Successfully retrieved {} active order items", responses.size());
                            return ApiResponse.success("Active order items retrieved successfully", responses);
                        })
                        .onFailure().invoke(e -> logger.error("Error finding active order items", e)));
    }

    @Override
    public Uni<ApiResponse<List<OrderItemResponseDeleteAt>>> findTrashed(FindAllOrderItemRequest request) {
        logger.info("Finding trashed order items with request: {}", request);

        return tracingMetrics.traceAndMeasure("findTrashedOrderItems", "find_trashed_order_items",
                () -> orderItemRepository.findTrashedOrderItems(request)
                        .map(paged -> {
                            List<OrderItemResponseDeleteAt> responses = paged.getData().stream()
                                    .map(OrderItemResponseDeleteAt::from)
                                    .collect(Collectors.toList());
                            logger.info("Successfully retrieved {} trashed order items", responses.size());
                            return ApiResponse.success("Trashed order items retrieved successfully", responses);
                        })
                        .onFailure().invoke(e -> logger.error("Error finding trashed order items", e)));
    }

    @Override
    public Uni<ApiResponse<List<OrderItemResponse>>> findByOrder(Long orderId) {
        logger.info("Finding order items by order ID: {}", orderId);

        return tracingMetrics.traceAndMeasure("findOrderItemByOrder", "find_order_items_by_order",
                Attributes.builder().put("order.id", orderId.toString()).build(),
                () -> orderItemRepository.findOrderItemByOrder(orderId)
                        .map(items -> {
                            List<OrderItemResponse> responses = items.stream()
                                    .map(OrderItemResponse::from)
                                    .collect(Collectors.toList());
                            logger.info("Successfully retrieved {} order items for order ID: {}", responses.size(),
                                    orderId);
                            return ApiResponse.success("Order items for order retrieved successfully", responses);
                        })
                        .onFailure()
                        .invoke(e -> logger.error("Error finding order items by order ID: {}", orderId, e)));
    }
}