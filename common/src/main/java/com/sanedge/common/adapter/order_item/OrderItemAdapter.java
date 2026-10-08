package com.sanedge.common.adapter.order_item;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Empty;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.order_item.MutinyOrderItemCommandServiceGrpc.MutinyOrderItemCommandServiceStub;
import pb.order_item.MutinyOrderItemQueryServiceGrpc.MutinyOrderItemQueryServiceStub;
import pb.order_item.OrderItemCommand.CalculateTotalPriceRequest;
import pb.order_item.OrderItemCommand.CreateOrderItemRecordRequest;
import pb.order_item.OrderItemCommand.UpdateOrderItemRecordRequest;
import pb.order_item.OrderItemCommon.ApiResponseOrderItem;
import pb.order_item.OrderItemCommon.FindByIdOrderItemRequest;
import pb.order_item.OrderItemCommon.OrderItemResponse;
import pb.order_item.OrderItemQuery.FindAllOrderItemRequest;

@ApplicationScoped
public class OrderItemAdapter implements OrderItemPort {

    @GrpcClient("order_item")
    MutinyOrderItemQueryServiceStub query;

    @GrpcClient("order_item")
    MutinyOrderItemCommandServiceStub command;

    @Override
    public Uni<List<OrderItem>> findOrderItemByOrder(int orderId) {
        return query.findOrderItemByOrder(FindByIdOrderItemRequest.newBuilder().setId(orderId).build())
                .map(resp -> {
                    List<OrderItem> items = new ArrayList<>(resp.getDataList().size());
                    for (OrderItemResponse item : resp.getDataList()) {
                        items.add(toModel(item));
                    }
                    return items;
                });
    }

    @Override
    public Uni<Integer> calculateTotalPrice(int orderId) {
        return command.calculateTotalPrice(CalculateTotalPriceRequest.newBuilder().setOrderId(orderId).build())
                .map(resp -> resp.getTotalPrice());
    }

    @Override
    public Uni<OrderItem> create(CreateData data) {
        return command.createOrderItem(CreateOrderItemRecordRequest.newBuilder()
                .setOrderId(data.orderId())
                .setProductId(data.productId())
                .setQuantity(data.quantity())
                .setPrice(data.price())
                .build())
                .map(resp -> requireData(resp, "Failed to create order item"));
    }

    @Override
    public Uni<OrderItem> update(UpdateData data) {
        return command.updateOrderItem(UpdateOrderItemRecordRequest.newBuilder()
                .setOrderItemId(data.orderItemId())
                .setQuantity(data.quantity())
                .setPrice(data.price())
                .build())
                .map(resp -> requireData(resp, "Failed to update order item " + data.orderItemId()));
    }

    @Override
    public Uni<OrderItem> trash(int orderItemId) {
        return command.trashOrderItem(FindByIdOrderItemRequest.newBuilder().setId(orderItemId).build())
                .map(resp -> requireData(resp, "Failed to trash order item " + orderItemId));
    }

    @Override
    public Uni<OrderItem> restore(int orderItemId) {
        return command.restoreOrderItem(FindByIdOrderItemRequest.newBuilder().setId(orderItemId).build())
                .map(resp -> requireData(resp, "Failed to restore order item " + orderItemId));
    }

    @Override
    public Uni<Boolean> deletePermanent(int orderItemId) {
        return command.deleteOrderItemPermanent(FindByIdOrderItemRequest.newBuilder().setId(orderItemId).build())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> deleteByOrderIdPermanent(int orderId) {
        return command.deleteOrderItemByOrderPermanent(FindByIdOrderItemRequest.newBuilder().setId(orderId).build())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> deleteByOrderIdRollback(int orderId) {
        return command.deleteOrderItemByOrderRollback(FindByIdOrderItemRequest.newBuilder().setId(orderId).build())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> restoreAll() {
        return command.restoreAllOrdersItem(Empty.getDefaultInstance())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> deleteAll() {
        return command.deleteAllPermanentOrdersItem(Empty.getDefaultInstance())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Paged<OrderItem>> findAll(int page, int pageSize) {
        return query.findAll(FindAllOrderItemRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build())
                .map(resp -> {
                    List<OrderItem> items = new ArrayList<>(resp.getDataList().size());
                    for (OrderItemResponse item : resp.getDataList()) {
                        items.add(toModel(item));
                    }
                    int total = resp.hasPagination() ? resp.getPagination().getTotalRecords() : items.size();
                    return Paged.of(items, total);
                });
    }

    private static OrderItem requireData(ApiResponseOrderItem resp, String message) {
        if (resp == null || !resp.hasData()) {
            throw new AdapterException(message);
        }
        return toModel(resp.getData());
    }

    static OrderItem toModel(OrderItemResponse item) {
        if (item == null) {
            return null;
        }
        return new OrderItem(
                item.getId(),
                item.getOrderId(),
                item.getProductId(),
                item.getQuantity(),
                item.getPrice(),
                ProtoTime.parse(item.getCreatedAt()),
                ProtoTime.parse(item.getUpdatedAt()));
    }
}
