package com.sanedge.gateway.dto;

import java.util.List;

public class ShippingAddressDto {
    public record ShippingResponse(
            int id,
            int orderId,
            String alamat,
            String provinsi,
            String negara,
            String kota,
            String shippingMethod,
            int shippingCost,
            String createdAt,
            String updatedAt) {
        public static ShippingResponse from(pb.shipping_address.ShippingAddressCommon.ShippingResponse proto) {
            return new ShippingResponse(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getAlamat(),
                    proto.getProvinsi(),
                    proto.getNegara(),
                    proto.getKota(),
                    proto.getShippingMethod(),
                    proto.getShippingCost(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static ShippingResponse from(pb.shipping_address.ShippingAddressCommon.ShippingResponseDeleteAt proto) {
            return new ShippingResponse(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getAlamat(),
                    proto.getProvinsi(),
                    proto.getNegara(),
                    proto.getKota(),
                    proto.getShippingMethod(),
                    proto.getShippingCost(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllShippingResponse(
            List<ShippingResponse> data,
            String status,
            String message) {
        public static FindAllShippingResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponsePaginationShipping proto) {
            return new FindAllShippingResponse(
                    proto.getDataList().stream().map(ShippingResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllShippingResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponsePaginationShippingDeleteAt proto) {
            return new FindAllShippingResponse(
                    proto.getDataList().stream().map(ShippingResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdShippingResponse(
            ShippingResponse data,
            String status,
            String message) {
        public static FindByIdShippingResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto) {
            return new FindByIdShippingResponse(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdShippingResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt proto) {
            return new FindByIdShippingResponse(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateShippingAddressRequest(
            int orderId,
            String alamat,
            String provinsi,
            String negara,
            String kota,
            String shippingMethod,
            int shippingCost) {}

    public record CreateShippingAddressResponse(
            ShippingResponse data,
            String status,
            String message) {
        public static CreateShippingAddressResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto) {
            return new CreateShippingAddressResponse(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateShippingAddressRequest(
            int orderId,
            String alamat,
            String provinsi,
            String negara,
            String kota,
            String shippingMethod,
            int shippingCost) {}

    public record UpdateShippingAddressResponse(
            ShippingResponse data,
            String status,
            String message) {
        public static UpdateShippingAddressResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto) {
            return new UpdateShippingAddressResponse(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationAddress(
            List<ShippingResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationAddress from(pb.shipping_address.ShippingAddressCommon.ApiResponsePaginationShipping proto) {
            return new ApiResponsePaginationAddress(
                    proto.getDataList().stream().map(ShippingResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationAddress from(pb.shipping_address.ShippingAddressCommon.ApiResponsePaginationShippingDeleteAt proto) {
            return new ApiResponsePaginationAddress(
                    proto.getDataList().stream().map(ShippingResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseAddress(
            ShippingResponse data,
            String status,
            String message) {
        public static ApiResponseAddress from(pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto) {
            return new ApiResponseAddress(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseAddress from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt proto) {
            return new ApiResponseAddress(
                    proto.hasData() ? ShippingResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.shipping_address.ShippingAddressCommon.ApiResponseShippingAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
