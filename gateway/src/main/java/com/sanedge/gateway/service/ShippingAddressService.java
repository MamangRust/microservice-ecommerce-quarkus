package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ShippingAddressDto;
import io.smallrye.mutiny.Uni;

public interface ShippingAddressService {
    Uni<ShippingAddressDto.ApiResponsePaginationAddress> findAll(int page, int size, String search);
    Uni<ShippingAddressDto.ApiResponseAddress> findByOrder(int orderId);
    Uni<ShippingAddressDto.ApiResponsePaginationAddress> findByActive(int page, int size, String search);
    Uni<ShippingAddressDto.ApiResponsePaginationAddress> findByTrashed(int page, int size, String search);
    Uni<ShippingAddressDto.ApiResponseAddress> findById(int id);
    Uni<ShippingAddressDto.ApiResponseAddress> create(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest body);
    Uni<ShippingAddressDto.ApiResponseAddress> update(int id, pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest body);
    Uni<ShippingAddressDto.ApiResponseAddress> delete(int id);
    Uni<ShippingAddressDto.ApiResponseAddress> restore(int id);
    Uni<ShippingAddressDto.SimpleResponse> deletePermanent(int id);
    Uni<ShippingAddressDto.SimpleResponse> restoreAll();
    Uni<ShippingAddressDto.SimpleResponse> deleteAllPermanent();
}
