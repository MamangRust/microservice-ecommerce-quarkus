package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.service.OrderService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class OrderResourceTest {
    @Mock
    OrderService orderService;
    private OrderResource orderResource;

    @BeforeEach
    void setUp() throws Exception {
        orderResource = new OrderResource();
        Field f = OrderResource.class.getDeclaredField("orderService");
        f.setAccessible(true);
        f.set(orderResource, orderService);
    }

    private OrderDto.OrderResponse mk(int id) {
        return new OrderDto.OrderResponse(id, 1, 1, 1000, "", "");
    }

    @Test
    void findAll_Success() {
        OrderDto.ApiResponsePaginationOrder dto = new OrderDto.ApiResponsePaginationOrder(
                List.of(mk(1)), "success", "ok");
        lenient().when(orderService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        OrderDto.ApiResponsePaginationOrderDeleteAt dto = new OrderDto.ApiResponsePaginationOrderDeleteAt(
                List.of(), "success", "ok");
        lenient().when(orderService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        OrderDto.ApiResponsePaginationOrderDeleteAt dto = new OrderDto.ApiResponsePaginationOrderDeleteAt(
                List.of(), "success", "ok");
        lenient().when(orderService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        OrderDto.ApiResponseOrder dto = new OrderDto.ApiResponseOrder(mk(1), "success", "ok");
        lenient().when(orderService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        OrderDto.ApiResponseOrder dto = new OrderDto.ApiResponseOrder(mk(1), "success", "ok");
        lenient().when(orderService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.order.OrderCommand.CreateOrderRequest req = pb.order.OrderCommand.CreateOrderRequest.newBuilder().build();
        Response r = orderResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        OrderDto.ApiResponseOrder dto = new OrderDto.ApiResponseOrder(mk(1), "success", "ok");
        lenient().when(orderService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.order.OrderCommand.UpdateOrderRequest req = pb.order.OrderCommand.UpdateOrderRequest.newBuilder().build();
        Response r = orderResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void updateTotalPrice_Success() {
        OrderDto.ApiResponseOrder dto = new OrderDto.ApiResponseOrder(mk(1), "success", "ok");
        lenient().when(orderService.updateTotalPrice(any())).thenReturn(Uni.createFrom().item(dto));
        pb.order.OrderCommand.UpdateOrderTotalPriceRequest req = pb.order.OrderCommand.UpdateOrderTotalPriceRequest.newBuilder().build();
        Response r = orderResource.updateTotalPrice(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        OrderDto.ApiResponseOrderDeleteAt dto = new OrderDto.ApiResponseOrderDeleteAt(mk(1), "success", "ok");
        lenient().when(orderService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        OrderDto.ApiResponseOrderDeleteAt dto = new OrderDto.ApiResponseOrderDeleteAt(mk(1), "success", "ok");
        lenient().when(orderService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        OrderDto.SimpleResponse dto = new OrderDto.SimpleResponse("success", "ok");
        lenient().when(orderService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        OrderDto.SimpleResponse dto = new OrderDto.SimpleResponse("success", "ok");
        lenient().when(orderService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        OrderDto.SimpleResponse dto = new OrderDto.SimpleResponse("success", "ok");
        lenient().when(orderService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyRevenue_Success() {
        OrderDto.ApiResponseOrderMonthly dto = new OrderDto.ApiResponseOrderMonthly(
                List.of(), "success", "ok");
        lenient().when(orderService.findMonthlyRevenue(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getMonthlyRevenue(2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getYearlyRevenue_Success() {
        OrderDto.ApiResponseOrderYearly dto = new OrderDto.ApiResponseOrderYearly(
                List.of(), "success", "ok");
        lenient().when(orderService.findYearlyRevenue(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getYearlyRevenue(2024).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyTotalRevenue_Success() {
        OrderDto.ApiResponseOrderMonthlyTotalRevenue dto = new OrderDto.ApiResponseOrderMonthlyTotalRevenue(
                List.of(), "success", "ok");
        lenient().when(orderService.findMonthlyTotalRevenue(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getMonthlyTotalRevenue(2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getYearlyTotalRevenue_Success() {
        OrderDto.ApiResponseOrderYearlyTotalRevenue dto = new OrderDto.ApiResponseOrderYearlyTotalRevenue(
                List.of(), "success", "ok");
        lenient().when(orderService.findYearlyTotalRevenue(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getYearlyTotalRevenue(2024).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyRevenueByMerchant_Success() {
        OrderDto.ApiResponseOrderMonthly dto = new OrderDto.ApiResponseOrderMonthly(
                List.of(), "success", "ok");
        lenient().when(orderService.findMonthlyRevenueByMerchant(anyInt(), anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getMonthlyRevenueByMerchant(1, 2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getYearlyRevenueByMerchant_Success() {
        OrderDto.ApiResponseOrderYearly dto = new OrderDto.ApiResponseOrderYearly(
                List.of(), "success", "ok");
        lenient().when(orderService.findYearlyRevenueByMerchant(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getYearlyRevenueByMerchant(1, 2024).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyTotalRevenueByMerchant_Success() {
        OrderDto.ApiResponseOrderMonthlyTotalRevenue dto = new OrderDto.ApiResponseOrderMonthlyTotalRevenue(
                List.of(), "success", "ok");
        lenient().when(orderService.findMonthlyTotalRevenueByMerchant(anyInt(), anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getMonthlyTotalRevenueByMerchant(1, 2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getYearlyTotalRevenueByMerchant_Success() {
        OrderDto.ApiResponseOrderYearlyTotalRevenue dto = new OrderDto.ApiResponseOrderYearlyTotalRevenue(
                List.of(), "success", "ok");
        lenient().when(orderService.findYearlyTotalRevenueByMerchant(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = orderResource.getYearlyTotalRevenueByMerchant(1, 2024).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
