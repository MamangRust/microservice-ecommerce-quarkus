package com.sanedge.shipping_address.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ShippingAddressCommand {
    Uni<ApiResponse<ShippingAddressResponse>> create(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest req);
    Uni<ApiResponse<ShippingAddressResponse>> update(pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest req);
    Uni<ApiResponse<ShippingAddressResponseDeleteAt>> trash(Integer shippingId);
    Uni<ApiResponse<ShippingAddressResponseDeleteAt>> restore(Integer shippingId);
    Uni<ApiResponse<Void>> deletePermanently(Integer shippingId);
    Uni<ApiResponse<Void>> restoreAll();
    Uni<ApiResponse<Void>> deleteAllPermanent();
}
