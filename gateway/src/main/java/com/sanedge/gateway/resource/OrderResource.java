package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.OrderService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderResource {

        @Inject
        OrderService orderService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all orders")
        public Uni<Response> findAll(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return orderService.findAll(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active orders")
        public Uni<Response> findByActive(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return orderService.findByActive(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed orders")
        public Uni<Response> findByTrashed(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return orderService.findByTrashed(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get order by ID")
        public Uni<Response> findById(@PathParam("id") int id) {
                return orderService.findById(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/create")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Create a new order")
        public Uni<Response> create(pb.order.OrderCommand.CreateOrderRequest body) {
                return orderService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @POST
        @Path("/update/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Update order")
        public Uni<Response> update(@PathParam("id") int id, pb.order.OrderCommand.UpdateOrderRequest body) {
                return orderService.update(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/update-total-price")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Update order total price")
        public Uni<Response> updateTotalPrice(pb.order.OrderCommand.UpdateOrderTotalPriceRequest body) {
                return orderService.updateTotalPrice(body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete an order")
        public Uni<Response> delete(@PathParam("id") int id) {
                return orderService.delete(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore a soft-deleted order")
        public Uni<Response> restore(@PathParam("id") int id) {
                return orderService.restore(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete an order")
        public Uni<Response> deletePermanent(@PathParam("id") int id) {
                return orderService.deletePermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted orders")
        public Uni<Response> restoreAll() {
                return orderService.restoreAll()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted orders")
        public Uni<Response> deleteAllPermanent() {
                return orderService.deleteAllPermanent()
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly revenue stats")
        public Uni<Response> getMonthlyRevenue(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return orderService.findMonthlyRevenue(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly revenue stats")
        public Uni<Response> getYearlyRevenue(@QueryParam("year") int year) {
                return orderService.findYearlyRevenue(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-total-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly total revenue stats")
        public Uni<Response> getMonthlyTotalRevenue(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return orderService.findMonthlyTotalRevenue(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-total-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly total revenue stats")
        public Uni<Response> getYearlyTotalRevenue(@QueryParam("year") int year) {
                return orderService.findYearlyTotalRevenue(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly revenue stats by merchant")
        public Uni<Response> getMonthlyRevenueByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return orderService.findMonthlyRevenueByMerchant(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly revenue stats by merchant")
        public Uni<Response> getYearlyRevenueByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return orderService.findYearlyRevenueByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-total-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly total revenue stats by merchant")
        public Uni<Response> getMonthlyTotalRevenueByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return orderService.findMonthlyTotalRevenueByMerchant(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-total-revenue")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly total revenue stats by merchant")
        public Uni<Response> getYearlyTotalRevenueByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return orderService.findYearlyTotalRevenueByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }
}
