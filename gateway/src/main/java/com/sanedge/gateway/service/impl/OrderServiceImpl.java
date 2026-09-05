package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.service.OrderService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {

    private static final Logger LOG = Logger.getLogger(OrderServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("order")
    pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub orderQueryService;

    @GrpcClient("order")
    pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub orderCommandService;

    @GrpcClient("statsreader")
    pb.order.stats.MutinyOrderRevenueServiceGrpc.MutinyOrderRevenueServiceStub orderRevenueService;

    @GrpcClient("statsreader")
    pb.order.stats.MutinyOrderTotalRevenueServiceGrpc.MutinyOrderTotalRevenueServiceStub orderTotalRevenueService;

    @GrpcClient("statsreader")
    pb.order.stats.MutinyOrderRevenueByMerchantGrpc.MutinyOrderRevenueByMerchantStub orderRevenueByMerchantStub;

    @GrpcClient("statsreader")
    pb.order.stats.MutinyOrderTotalRevenueByMerchantGrpc.MutinyOrderTotalRevenueByMerchantStub orderTotalRevenueByMerchantStub;

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrder> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.findAll", () -> orderQueryService.findAll(
                pb.order.OrderQuery.FindAllOrderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(OrderDto.ApiResponsePaginationOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.findByActive", () -> orderQueryService.findByActive(
                pb.order.OrderQuery.FindAllOrderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(OrderDto.ApiResponsePaginationOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.findByTrashed", () -> orderQueryService.findByTrashed(
                pb.order.OrderQuery.FindAllOrderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(OrderDto.ApiResponsePaginationOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> findById(int id) {
        return telemetryHelper.traceAndMetric("order.findById", () -> orderQueryService.findById(
                pb.order.OrderCommon.FindByIdOrderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> create(pb.order.OrderCommand.CreateOrderRequest body) {
        return telemetryHelper.traceAndMetric("order.create", () -> orderCommandService.create(body)
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create order: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> update(int id, pb.order.OrderCommand.UpdateOrderRequest body) {
        pb.order.OrderCommand.UpdateOrderRequest req = pb.order.OrderCommand.UpdateOrderRequest.newBuilder(body)
                .setOrderId(id)
                .build();
        return telemetryHelper.traceAndMetric("order.update", () -> orderCommandService.update(req)
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderDeleteAt> delete(int id) {
        return telemetryHelper.traceAndMetric("order.delete", () -> orderCommandService.trashedOrder(
                pb.order.OrderCommon.FindByIdOrderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(OrderDto.ApiResponseOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderDeleteAt> restore(int id) {
        return telemetryHelper.traceAndMetric("order.restore", () -> orderCommandService.restoreOrder(
                pb.order.OrderCommon.FindByIdOrderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(OrderDto.ApiResponseOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("order.deletePermanent", () -> orderCommandService.deleteOrderPermanent(
                pb.order.OrderCommon.FindByIdOrderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(OrderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> updateTotalPrice(pb.order.OrderCommand.UpdateOrderTotalPriceRequest body) {
        return telemetryHelper.traceAndMetric("order.updateTotalPrice", () -> orderCommandService.updateTotalPrice(body)
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update order total price: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("order.restoreAll", () -> orderCommandService.restoreAllOrder(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(OrderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("order.deleteAllPermanent", () -> orderCommandService.deleteAllOrderPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(OrderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthly> findMonthlyRevenue(int year, int month) {
        return telemetryHelper.traceAndMetric("order.findMonthlyRevenue", () -> orderRevenueService.findMonthlyRevenue(
                pb.order.stats.OrderRevenue.FindYearOrder.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(OrderDto.ApiResponseOrderMonthly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly revenue: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearly> findYearlyRevenue(int year) {
        return telemetryHelper.traceAndMetric("order.findYearlyRevenue", () -> orderRevenueService.findYearlyRevenue(
                pb.order.stats.OrderRevenue.FindYearOrder.newBuilder()
                        .setYear(year)
                        .build())
                .map(OrderDto.ApiResponseOrderYearly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly revenue: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenue(int year, int month) {
        return telemetryHelper.traceAndMetric("order.findMonthlyTotalRevenue", () -> orderTotalRevenueService.findMonthlyTotalRevenue(
                pb.order.stats.OrderTotalRevenue.FindYearMonthTotalRevenue.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(OrderDto.ApiResponseOrderMonthlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly total revenue: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenue(int year) {
        return telemetryHelper.traceAndMetric("order.findYearlyTotalRevenue", () -> orderTotalRevenueService.findYearlyTotalRevenue(
                pb.order.stats.OrderTotalRevenue.FindYearTotalRevenue.newBuilder()
                        .setYear(year)
                        .build())
                .map(OrderDto.ApiResponseOrderYearlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly total revenue: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthly> findMonthlyRevenueByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("order.findMonthlyRevenueByMerchant", () -> orderRevenueByMerchantStub.findMonthlyRevenueByMerchant(
                pb.order.stats.OrderRevenue.FindYearOrderByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(OrderDto.ApiResponseOrderMonthly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly revenue by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearly> findYearlyRevenueByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("order.findYearlyRevenueByMerchant", () -> orderRevenueByMerchantStub.findYearlyRevenueByMerchant(
                pb.order.stats.OrderRevenue.FindYearOrderByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .build())
                .map(OrderDto.ApiResponseOrderYearly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly revenue by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("order.findMonthlyTotalRevenueByMerchant", () -> orderTotalRevenueByMerchantStub.findMonthlyTotalRevenueByMerchant(
                pb.order.stats.OrderTotalRevenue.FindYearMonthTotalRevenueByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(OrderDto.ApiResponseOrderMonthlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly total revenue by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("order.findYearlyTotalRevenueByMerchant", () -> orderTotalRevenueByMerchantStub.findYearlyTotalRevenueByMerchant(
                pb.order.stats.OrderTotalRevenue.FindYearTotalRevenueByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .build())
                .map(OrderDto.ApiResponseOrderYearlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly total revenue by merchant: " + throwable.getMessage(), throwable)));
    }
}
