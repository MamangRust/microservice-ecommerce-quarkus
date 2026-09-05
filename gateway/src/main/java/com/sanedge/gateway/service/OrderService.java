package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.OrderDto;
import io.smallrye.mutiny.Uni;

public interface OrderService {
    Uni<OrderDto.ApiResponsePaginationOrder> findAll(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> findByActive(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> findByTrashed(int page, int size, String search);
    Uni<OrderDto.ApiResponseOrder> findById(int id);

    Uni<OrderDto.ApiResponseOrder> create(pb.order.OrderCommand.CreateOrderRequest body);
    Uni<OrderDto.ApiResponseOrder> update(int id, pb.order.OrderCommand.UpdateOrderRequest body);
    Uni<OrderDto.ApiResponseOrderDeleteAt> delete(int id);
    Uni<OrderDto.ApiResponseOrderDeleteAt> restore(int id);
    Uni<OrderDto.SimpleResponse> deletePermanent(int id);
    Uni<OrderDto.ApiResponseOrder> updateTotalPrice(pb.order.OrderCommand.UpdateOrderTotalPriceRequest body);
    Uni<OrderDto.SimpleResponse> restoreAll();
    Uni<OrderDto.SimpleResponse> deleteAllPermanent();

    Uni<OrderDto.ApiResponseOrderMonthly> findMonthlyRevenue(int year, int month);
    Uni<OrderDto.ApiResponseOrderYearly> findYearlyRevenue(int year);
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenue(int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenue(int year);

    Uni<OrderDto.ApiResponseOrderMonthly> findMonthlyRevenueByMerchant(int merchantId, int year, int month);
    Uni<OrderDto.ApiResponseOrderYearly> findYearlyRevenueByMerchant(int merchantId, int year);
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueByMerchant(int merchantId, int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueByMerchant(int merchantId, int year);
}
