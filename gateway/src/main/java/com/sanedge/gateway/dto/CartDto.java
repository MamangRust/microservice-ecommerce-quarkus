package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class CartDto {

    public record CartItemResponse(
            int id,
            int productId,
            int quantity,
            double price,
            String createdAt,
            String updatedAt) {
        public static CartItemResponse from(pb.cart.CartCommon.CartResponse proto) {
            return new CartItemResponse(
                    proto.getId(),
                    proto.getProductId(),
                    proto.getQuantity(),
                    proto.getPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    public record ApiResponseCart(
            String status,
            String message,
            CartItemResponse data) {
        public static ApiResponseCart from(pb.cart.CartCommon.ApiResponseCart proto) {
            return new ApiResponseCart(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? CartItemResponse.from(proto.getData()) : null);
        }
    }

    public record ApiResponsePaginationCart(
            String status,
            String message,
            List<CartItemResponse> data,
            PaginationMeta paginationMeta) {
        public static ApiResponsePaginationCart from(pb.cart.CartCommon.ApiResponsePaginationCart proto) {
            List<CartItemResponse> list = proto.getDataList().stream()
                    .map(CartItemResponse::from)
                    .collect(Collectors.toList());
            return new ApiResponsePaginationCart(
                    proto.getStatus(),
                    proto.getMessage(),
                    list,
                    proto.hasPagination() ? PaginationMeta.from(proto.getPagination()) : null);
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.cart.CartCommon.ApiResponseCartDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.cart.CartCommon.ApiResponseCartAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
