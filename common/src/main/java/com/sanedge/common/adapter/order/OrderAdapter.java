package com.sanedge.common.adapter.order;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.Order;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub;
import pb.order.OrderCommon.FindByIdOrderRequest;
import pb.order.OrderCommon.OrderResponse;
import pb.order.OrderQuery.FindAllOrderRequest;

@ApplicationScoped
public class OrderAdapter implements OrderPort {

    @GrpcClient("order")
    MutinyOrderQueryServiceStub client;

    @Override
    public Uni<Order> findById(int orderId) {
        return client.findById(FindByIdOrderRequest.newBuilder().setId(orderId).build())
                .map(resp -> toModel(resp.getData()));
    }

    @Override
    public Uni<Paged<Order>> findAll(int page, int pageSize) {
        return client.findAll(FindAllOrderRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build())
                .map(resp -> {
                    List<Order> items = new ArrayList<>(resp.getDataList().size());
                    for (OrderResponse o : resp.getDataList()) {
                        items.add(toModel(o));
                    }
                    int total = resp.hasPagination() ? resp.getPagination().getTotalRecords() : items.size();
                    return Paged.of(items, total);
                });
    }

    static Order toModel(OrderResponse o) {
        if (o == null) {
            return null;
        }
        return new Order(
                o.getId(),
                o.getMerchantId(),
                o.getUserId(),
                o.getTotalPrice(),
                ProtoTime.parse(o.getCreatedAt()),
                ProtoTime.parse(o.getUpdatedAt()));
    }
}
