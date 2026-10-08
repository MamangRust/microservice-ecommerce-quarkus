package com.sanedge.common.adapter.shipping_address;

import com.google.protobuf.Empty;
import com.sanedge.common.adapter.model.ShippingAddress;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.ProtoTime;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub;
import pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub;
import pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest;
import pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest;
import pb.shipping_address.ShippingAddressCommon.ApiResponseShipping;
import pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest;
import pb.shipping_address.ShippingAddressCommon.ShippingResponse;

@ApplicationScoped
public class ShippingAddressAdapter implements ShippingAddressPort {

    @GrpcClient("shipping_address")
    MutinyShippingQueryServiceStub query;

    @GrpcClient("shipping_address")
    MutinyShippingCommandServiceStub command;

    @Override
    public Uni<ShippingAddress> findById(int shippingId) {
        return query.findById(FindByIdShippingRequest.newBuilder().setId(shippingId).build())
                .map(resp -> toModel(resp.getData()));
    }

    @Override
    public Uni<ShippingAddress> findByOrder(int orderId) {
        return query.findByOrder(FindByIdShippingRequest.newBuilder().setId(orderId).build())
                .map(resp -> toModel(resp.getData()));
    }

    @Override
    public Uni<ShippingAddress> create(CreateData data) {
        return command.createShipping(CreateShippingAddressRequest.newBuilder()
                .setOrderId(data.orderId())
                .setAlamat(data.alamat())
                .setProvinsi(data.provinsi())
                .setKota(data.kota())
                .setNegara(data.negara())
                .setCourier(data.courier())
                .setShippingMethod(data.shippingMethod())
                .setShippingCost(data.shippingCost())
                .build())
                .map(resp -> requireData(resp, "Failed to create shipping address"));
    }

    @Override
    public Uni<ShippingAddress> update(UpdateData data) {
        return command.updateShipping(UpdateShippingAddressRequest.newBuilder()
                .setShippingId(data.shippingId())
                .setAlamat(data.alamat())
                .setProvinsi(data.provinsi())
                .setKota(data.kota())
                .setNegara(data.negara())
                .setCourier(data.courier())
                .setShippingMethod(data.shippingMethod())
                .setShippingCost(data.shippingCost())
                .build())
                .map(resp -> requireData(resp, "Failed to update shipping address"));
    }

    @Override
    public Uni<Boolean> deleteByOrderIdPermanent(int orderId) {
        return command.deleteShippingByOrderPermanent(FindByIdShippingRequest.newBuilder().setId(orderId).build())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> deleteAll() {
        return command.deleteAllShippingPermanent(Empty.getDefaultInstance())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    private static ShippingAddress requireData(ApiResponseShipping resp, String message) {
        if (resp == null || !resp.hasData()) {
            throw new AdapterException(message);
        }
        return toModel(resp.getData());
    }

    static ShippingAddress toModel(ShippingResponse s) {
        if (s == null) {
            return null;
        }
        return new ShippingAddress(
                s.getId(),
                s.getOrderId(),
                s.getAlamat(),
                s.getProvinsi(),
                s.getNegara(),
                s.getKota(),
                s.getShippingMethod(),
                s.getShippingCost(),
                ProtoTime.parse(s.getCreatedAt()),
                ProtoTime.parse(s.getUpdatedAt()));
    }
}
