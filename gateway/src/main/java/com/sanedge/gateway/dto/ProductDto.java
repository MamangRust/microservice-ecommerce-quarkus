package com.sanedge.gateway.dto;

import java.util.List;

public class ProductDto {
    public record ProductResponse(
            int id,
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            float rating,
            String slugProduct,
            String imageProduct,
            String createdAt,
            String updatedAt) {
        public static ProductResponse from(pb.product.ProductCommon.ProductResponse proto) {
            return new ProductResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCategoryId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getPrice(),
                    proto.getCountInStock(),
                    proto.getBrand(),
                    proto.getWeight(),
                    proto.getRating(),
                    proto.getSlugProduct(),
                    proto.getImageProduct(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static ProductResponse from(pb.product.ProductCommon.ProductResponseDeleteAt proto) {
            return new ProductResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCategoryId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getPrice(),
                    proto.getCountInStock(),
                    proto.getBrand(),
                    proto.getWeight(),
                    proto.getRating(),
                    proto.getSlugProduct(),
                    proto.getImageProduct(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllProductResponse(
            List<ProductResponse> data,
            String status,
            String message) {
        public static FindAllProductResponse from(pb.product.ProductCommon.ApiResponsePaginationProduct proto) {
            return new FindAllProductResponse(
                    proto.getDataList().stream().map(ProductResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllProductResponse from(pb.product.ProductCommon.ApiResponsePaginationProductDeleteAt proto) {
            return new FindAllProductResponse(
                    proto.getDataList().stream().map(ProductResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdProductResponse(
            ProductResponse data,
            String status,
            String message) {
        public static FindByIdProductResponse from(pb.product.ProductCommon.ApiResponseProduct proto) {
            return new FindByIdProductResponse(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdProductResponse from(pb.product.ProductCommon.ApiResponseProductDeleteAt proto) {
            return new FindByIdProductResponse(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateProductRequest(
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            String slugProduct,
            String imageProduct) {}

    public record CreateProductResponse(
            ProductResponse data,
            String status,
            String message) {
        public static CreateProductResponse from(pb.product.ProductCommon.ApiResponseProduct proto) {
            return new CreateProductResponse(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateProductRequest(
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            String slugProduct,
            String imageProduct) {}

    public record UpdateProductResponse(
            ProductResponse data,
            String status,
            String message) {
        public static UpdateProductResponse from(pb.product.ProductCommon.ApiResponseProduct proto) {
            return new UpdateProductResponse(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.product.ProductCommon.ApiResponseProductDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.product.ProductCommon.ApiResponseProductAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationProduct(
            List<ProductResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationProduct from(pb.product.ProductCommon.ApiResponsePaginationProduct proto) {
            return new ApiResponsePaginationProduct(
                    proto.getDataList().stream().map(ProductResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationProduct from(pb.product.ProductCommon.ApiResponsePaginationProductDeleteAt proto) {
            return new ApiResponsePaginationProduct(
                    proto.getDataList().stream().map(ProductResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseProduct(
            ProductResponse data,
            String status,
            String message) {
        public static ApiResponseProduct from(pb.product.ProductCommon.ApiResponseProduct proto) {
            return new ApiResponseProduct(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseProduct from(pb.product.ProductCommon.ApiResponseProductDeleteAt proto) {
            return new ApiResponseProduct(
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.product.ProductCommon.ApiResponseProductDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.product.ProductCommon.ApiResponseProductAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
