package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.MerchantService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Merchants", description = "Merchant management endpoints")
public class MerchantResource {

        @Inject
        MerchantService merchantService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all merchants")
        public Uni<Response> listMerchants(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantService.listMerchants(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get merchant by ID")
        public Uni<Response> getMerchant(@PathParam("id") int id) {
                return merchantService.getMerchant(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active merchants")
        public Uni<Response> getActiveMerchants(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantService.getActiveMerchants(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed merchants")
        public Uni<Response> getTrashedMerchants(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantService.getTrashedMerchants(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Create a new merchant")
        public Uni<Response> createMerchant(pb.merchant.MerchantCommand.CreateMerchantRequest body) {
                return merchantService.createMerchant(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @PUT
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Update merchant")
        public Uni<Response> updateMerchant(@PathParam("id") int id,
                        pb.merchant.MerchantCommand.UpdateMerchantRequest body) {
                return merchantService.updateMerchant(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @PUT
        @Path("/{id}/status")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Update merchant status")
        public Uni<Response> updateMerchantStatus(@PathParam("id") int id,
                        pb.merchant.MerchantCommand.UpdateMerchantStatusRequest body) {
                return merchantService.updateMerchantStatus(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete a merchant")
        public Uni<Response> deleteMerchant(@PathParam("id") int id) {
                return merchantService.deleteMerchant(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore a soft-deleted merchant")
        public Uni<Response> restoreMerchant(@PathParam("id") int id) {
                return merchantService.restoreMerchant(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete a merchant")
        public Uni<Response> deleteMerchantPermanent(@PathParam("id") int id) {
                return merchantService.deleteMerchantPermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted merchants")
        public Uni<Response> restoreAllMerchant() {
                return merchantService.restoreAllMerchant()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted merchants")
        public Uni<Response> deleteAllMerchantPermanent() {
                return merchantService.deleteAllMerchantPermanent()
                                .map(dto -> Response.ok(dto).build());
        }
}
