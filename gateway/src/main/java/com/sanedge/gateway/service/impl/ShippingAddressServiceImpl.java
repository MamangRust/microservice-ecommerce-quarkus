package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.ShippingAddressDto;
import com.sanedge.gateway.service.ShippingAddressService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ShippingAddressServiceImpl implements ShippingAddressService {

    private static final Logger LOG = Logger.getLogger(ShippingAddressServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("shipping_address")
    pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub shippingQueryService;

    @GrpcClient("shipping_address")
    pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub shippingCommandService;

    @Override
    public Uni<ShippingAddressDto.ApiResponsePaginationAddress> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("shipping.address.findAll", () -> shippingQueryService.findAll(
                pb.shipping_address.ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ShippingAddressDto.ApiResponsePaginationAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all shipping addresses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> findByOrder(int orderId) {
        return telemetryHelper.traceAndMetric("shipping.address.findByOrder", () -> shippingQueryService.findByOrder(
                pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                        .setId(orderId)
                        .build())
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find shipping address by order " + orderId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponsePaginationAddress> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("shipping.address.findByActive", () -> shippingQueryService.findByActive(
                pb.shipping_address.ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ShippingAddressDto.ApiResponsePaginationAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active shipping addresses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponsePaginationAddress> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("shipping.address.findByTrashed", () -> shippingQueryService.findByTrashed(
                pb.shipping_address.ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ShippingAddressDto.ApiResponsePaginationAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed shipping addresses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> findById(int id) {
        return telemetryHelper.traceAndMetric("shipping.address.findById", () -> shippingQueryService.findById(
                pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find shipping address " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> create(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest body) {
        return telemetryHelper.traceAndMetric("shipping.address.create", () -> shippingCommandService.createShipping(body)
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create shipping address: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> update(int id, pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest body) {
        pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest req = pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest.newBuilder(body)
                .setShippingId(id)
                .build();
        return telemetryHelper.traceAndMetric("shipping.address.update", () -> shippingCommandService.updateShipping(req)
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update shipping address " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> delete(int id) {
        return telemetryHelper.traceAndMetric("shipping.address.delete", () -> shippingCommandService.trashedShipping(
                pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete shipping address " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.ApiResponseAddress> restore(int id) {
        return telemetryHelper.traceAndMetric("shipping.address.restore", () -> shippingCommandService.restoreShipping(
                pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ShippingAddressDto.ApiResponseAddress::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore shipping address " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("shipping.address.deletePermanent", () -> shippingCommandService.deleteShippingPermanent(
                pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ShippingAddressDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete shipping address " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("shipping.address.restoreAll", () -> shippingCommandService.restoreAllShipping(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ShippingAddressDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all shipping addresses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ShippingAddressDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("shipping.address.deleteAllPermanent", () -> shippingCommandService.deleteAllShippingPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ShippingAddressDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all shipping addresses: " + throwable.getMessage(), throwable)));
    }
}
