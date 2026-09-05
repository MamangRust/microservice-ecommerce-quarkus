package com.sanedge.gateway.resource;

import jakarta.annotation.security.RolesAllowed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.TransactionService;

import io.smallrye.mutiny.Uni;
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

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionResource {

        @Inject
        TransactionService transactionService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all transactions")
        public Uni<Response> findAll(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return transactionService.findAll(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/merchant/{merchantId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List transactions by merchant")
        public Uni<Response> findByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return transactionService.findByMerchant(merchantId, page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get transaction by ID")
        public Uni<Response> findById(@PathParam("id") int id) {
                return transactionService.findById(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/order/{orderId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get transaction by Order ID")
        public Uni<Response> findByOrderId(@PathParam("orderId") int orderId) {
                return transactionService.findByOrderId(orderId)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active transactions")
        public Uni<Response> findByActive(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return transactionService.findByActive(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed transactions")
        public Uni<Response> findByTrashed(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return transactionService.findByTrashed(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/create")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Create a new transaction")
        public Uni<Response> create(pb.transaction.TransactionCommand.CreateTransactionRequest body) {
                return transactionService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @POST
        @Path("/update/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Update transaction")
        public Uni<Response> update(@PathParam("id") int id,
                        pb.transaction.TransactionCommand.UpdateTransactionRequest body) {
                return transactionService.update(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete a transaction")
        public Uni<Response> delete(@PathParam("id") int id) {
                return transactionService.delete(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore a soft-deleted transaction")
        public Uni<Response> restore(@PathParam("id") int id) {
                return transactionService.restore(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete a transaction")
        public Uni<Response> deletePermanent(@PathParam("id") int id) {
                return transactionService.deletePermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted transactions")
        public Uni<Response> restoreAll() {
                return transactionService.restoreAll()
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/order/{orderId}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete transactions by Order ID")
        public Uni<Response> deleteTransactionByOrderPermanent(@PathParam("orderId") int orderId) {
                return transactionService.deleteTransactionByOrderPermanent(orderId)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted transactions")
        public Uni<Response> deleteAllPermanent() {
                return transactionService.deleteAllPermanent()
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-amount-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction success amount stats")
        public Uni<Response> getMonthlyAmountSuccess(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyAmountSuccess(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-amount-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction success amount stats")
        public Uni<Response> getYearlyAmountSuccess(@QueryParam("year") int year) {
                return transactionService.getYearlyAmountSuccess(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-amount-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction failed amount stats")
        public Uni<Response> getMonthlyAmountFailed(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyAmountFailed(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-amount-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction failed amount stats")
        public Uni<Response> getYearlyAmountFailed(@QueryParam("year") int year) {
                return transactionService.getYearlyAmountFailed(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-method-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction success method stats")
        public Uni<Response> getMonthlyTransactionMethodSuccess(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyTransactionMethodSuccess(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-method-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction success method stats")
        public Uni<Response> getYearlyTransactionMethodSuccess(@QueryParam("year") int year) {
                return transactionService.getYearlyTransactionMethodSuccess(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/monthly-method-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction failed method stats")
        public Uni<Response> getMonthlyTransactionMethodFailed(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyTransactionMethodFailed(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/yearly-method-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction failed method stats")
        public Uni<Response> getYearlyTransactionMethodFailed(@QueryParam("year") int year) {
                return transactionService.getYearlyTransactionMethodFailed(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-amount-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction success amount stats by merchant")
        public Uni<Response> getMonthlyAmountSuccessByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyAmountSuccessByMerchant(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-amount-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction success amount stats by merchant")
        public Uni<Response> getYearlyAmountSuccessByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return transactionService.getYearlyAmountSuccessByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-amount-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction failed amount stats by merchant")
        public Uni<Response> getMonthlyAmountFailedByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyAmountFailedByMerchant(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-amount-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction failed amount stats by merchant")
        public Uni<Response> getYearlyAmountFailedByMerchant(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return transactionService.getYearlyAmountFailedByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-method-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction success method stats by merchant")
        public Uni<Response> getMonthlyTransactionMethodByMerchantSuccess(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyTransactionMethodByMerchantSuccess(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-method-success")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction success method stats by merchant")
        public Uni<Response> getYearlyTransactionMethodByMerchantSuccess(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return transactionService.getYearlyTransactionMethodByMerchantSuccess(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/monthly-method-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly transaction failed method stats by merchant")
        public Uni<Response> getMonthlyTransactionMethodByMerchantFailed(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return transactionService.getMonthlyTransactionMethodByMerchantFailed(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/stats/merchant/{merchantId}/yearly-method-failed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly transaction failed method stats by merchant")
        public Uni<Response> getYearlyTransactionMethodByMerchantFailed(
                        @PathParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return transactionService.getYearlyTransactionMethodByMerchantFailed(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }
}
