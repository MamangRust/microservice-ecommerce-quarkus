package com.sanedge.gateway.dto;

import java.util.List;

public class OrderDto {
    public record OrderResponse(
            int id,
            int merchantId,
            int userId,
            int totalPrice,
            String createdAt,
            String updatedAt) {
        public static OrderResponse from(pb.order.OrderCommon.OrderResponse proto) {
            return new OrderResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getUserId(),
                    proto.getTotalPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static OrderResponse from(pb.order.OrderCommon.OrderResponseDeleteAt proto) {
            return new OrderResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getUserId(),
                    proto.getTotalPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllOrderResponse(
            List<OrderResponse> data,
            String status,
            String message) {
        public static FindAllOrderResponse from(pb.order.OrderCommon.ApiResponsePaginationOrder proto) {
            return new FindAllOrderResponse(
                    proto.getDataList().stream().map(OrderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllOrderResponse from(pb.order.OrderCommon.ApiResponsePaginationOrderDeleteAt proto) {
            return new FindAllOrderResponse(
                    proto.getDataList().stream().map(OrderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdOrderResponse(
            OrderResponse data,
            String status,
            String message) {
        public static FindByIdOrderResponse from(pb.order.OrderCommon.ApiResponseOrder proto) {
            return new FindByIdOrderResponse(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdOrderResponse from(pb.order.OrderCommon.ApiResponseOrderDeleteAt proto) {
            return new FindByIdOrderResponse(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateOrderRequest(
            int merchantId,
            int userId,
            int totalPrice) {}

    public record CreateOrderResponse(
            OrderResponse data,
            String status,
            String message) {
        public static CreateOrderResponse from(pb.order.OrderCommon.ApiResponseOrder proto) {
            return new CreateOrderResponse(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateOrderRequest(
            int merchantId,
            int userId,
            int totalPrice) {}

    public record UpdateOrderResponse(
            OrderResponse data,
            String status,
            String message) {
        public static UpdateOrderResponse from(pb.order.OrderCommon.ApiResponseOrder proto) {
            return new UpdateOrderResponse(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.order.OrderCommon.ApiResponseOrderDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.order.OrderCommon.ApiResponseOrderAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    // STATS RECORDS
    public record OrderMonthlyResponse(
            String month,
            int orderCount,
            long totalRevenue,
            int totalItemsSold) {
        public static OrderMonthlyResponse from(pb.order.OrderCommon.OrderMonthlyResponse proto) {
            return new OrderMonthlyResponse(
                    proto.getMonth(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold()
            );
        }
    }

    public record ApiResponseOrderMonthly(
            List<OrderMonthlyResponse> data,
            String status,
            String message) {
        public static ApiResponseOrderMonthly from(pb.order.OrderCommon.ApiResponseOrderMonthly proto) {
            return new ApiResponseOrderMonthly(
                    proto.getDataList().stream().map(OrderMonthlyResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record OrderYearlyResponse(
            String year,
            int orderCount,
            long totalRevenue,
            int totalItemsSold,
            int activeCashiers,
            int uniqueProductsSold) {
        public static OrderYearlyResponse from(pb.order.OrderCommon.OrderYearlyResponse proto) {
            return new OrderYearlyResponse(
                    proto.getYear(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold(),
                    proto.getActiveCashiers(),
                    proto.getUniqueProductsSold()
            );
        }
    }

    public record ApiResponseOrderYearly(
            List<OrderYearlyResponse> data,
            String status,
            String message) {
        public static ApiResponseOrderYearly from(pb.order.OrderCommon.ApiResponseOrderYearly proto) {
            return new ApiResponseOrderYearly(
                    proto.getDataList().stream().map(OrderYearlyResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationOrder(
            List<OrderResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationOrder from(pb.order.OrderCommon.ApiResponsePaginationOrder proto) {
            return new ApiResponsePaginationOrder(
                    proto.getDataList().stream().map(OrderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationOrderDeleteAt(
            List<OrderResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationOrderDeleteAt from(pb.order.OrderCommon.ApiResponsePaginationOrderDeleteAt proto) {
            return new ApiResponsePaginationOrderDeleteAt(
                    proto.getDataList().stream().map(OrderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseOrder(
            OrderResponse data,
            String status,
            String message) {
        public static ApiResponseOrder from(pb.order.OrderCommon.ApiResponseOrder proto) {
            return new ApiResponseOrder(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseOrderDeleteAt(
            OrderResponse data,
            String status,
            String message) {
        public static ApiResponseOrderDeleteAt from(pb.order.OrderCommon.ApiResponseOrderDeleteAt proto) {
            return new ApiResponseOrderDeleteAt(
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record OrderMonthlyTotalRevenueResponse(
            String year,
            String month,
            int orderCount,
            long totalRevenue,
            int totalItemsSold) {
        public static OrderMonthlyTotalRevenueResponse from(pb.order.OrderCommon.OrderMonthlyTotalRevenueResponse proto) {
            return new OrderMonthlyTotalRevenueResponse(
                    proto.getYear(),
                    proto.getMonth(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold()
            );
        }
    }

    public record OrderYearlyTotalRevenueResponse(
            String year,
            int orderCount,
            long totalRevenue,
            int totalItemsSold,
            int activeCashiers,
            int uniqueProductsSold) {
        public static OrderYearlyTotalRevenueResponse from(pb.order.OrderCommon.OrderYearlyTotalRevenueResponse proto) {
            return new OrderYearlyTotalRevenueResponse(
                    proto.getYear(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold(),
                    proto.getActiveCashiers(),
                    proto.getUniqueProductsSold()
            );
        }
    }

    public record ApiResponseOrderMonthlyTotalRevenue(
            List<OrderMonthlyTotalRevenueResponse> data,
            String status,
            String message) {
        public static ApiResponseOrderMonthlyTotalRevenue from(pb.order.OrderCommon.ApiResponseOrderMonthlyTotalRevenue proto) {
            return new ApiResponseOrderMonthlyTotalRevenue(
                    proto.getDataList().stream().map(OrderMonthlyTotalRevenueResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseOrderYearlyTotalRevenue(
            List<OrderYearlyTotalRevenueResponse> data,
            String status,
            String message) {
        public static ApiResponseOrderYearlyTotalRevenue from(pb.order.OrderCommon.ApiResponseOrderYearlyTotalRevenue proto) {
            return new ApiResponseOrderYearlyTotalRevenue(
                    proto.getDataList().stream().map(OrderYearlyTotalRevenueResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.order.OrderCommon.ApiResponseOrderDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.order.OrderCommon.ApiResponseOrderAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
