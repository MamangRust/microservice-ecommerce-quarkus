package com.sanedge.shipping_address.service;

import java.util.List;

import com.sanedge.shipping_address.domain.requests.FindAllShippingAddress;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ShippingAddressQueryService {
    Uni<ApiResponsePagination<List<ShippingAddressResponse>>> findAll(FindAllShippingAddress req);
    Uni<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>> findByActive(FindAllShippingAddress req);
    Uni<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>> findByTrashed(FindAllShippingAddress req);
    Uni<ApiResponse<ShippingAddressResponse>> findById(Integer shippingId);
    Uni<ApiResponse<ShippingAddressResponse>> findByOrder(Integer orderId);
}
