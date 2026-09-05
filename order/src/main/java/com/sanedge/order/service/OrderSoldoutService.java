package com.sanedge.order.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order.domain.response.OrderMonthlyResponse;
import com.sanedge.order.domain.response.OrderYearlyResponse;

import io.smallrye.mutiny.Uni;

public interface OrderSoldoutService {
    Uni<ApiResponse<List<OrderMonthlyResponse>>> findMonthlyOrders(Integer year, Integer month);
    Uni<ApiResponse<List<OrderYearlyResponse>>> findYearlyOrders(Integer year);
}
