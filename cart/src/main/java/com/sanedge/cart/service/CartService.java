package com.sanedge.cart.service;

import java.util.List;

import com.sanedge.cart.domain.requests.CreateCartRequest;
import com.sanedge.cart.domain.requests.DeleteCartRequest;
import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.cart.domain.response.CartResponse;

import io.smallrye.mutiny.Uni;

public interface CartService {
    Uni<ApiResponsePagination<List<CartResponse>>> findAll(FindAllCartsRequest request);
    Uni<ApiResponse<CartResponse>> createCart(CreateCartRequest request);
    Uni<ApiResponse<Void>> deletePermanent(Long cartId);
    Uni<ApiResponse<Void>> deleteAllPermanently(DeleteCartRequest request);
}
