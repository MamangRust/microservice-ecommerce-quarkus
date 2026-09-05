package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CartDto;
import io.smallrye.mutiny.Uni;

public interface CartService {
    Uni<CartDto.ApiResponsePaginationCart> findAll(int userId, int page, int size, String search);
    Uni<CartDto.ApiResponseCart> create(pb.cart.CartCommand.CreateCartRequest body);
    Uni<CartDto.SimpleResponse> delete(int cartId, int userId);
    Uni<CartDto.SimpleResponse> deleteAll(int userId, java.util.List<Integer> cartIds);
}
