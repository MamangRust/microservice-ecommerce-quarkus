package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.MerchantBusinessService;

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

@Path("/api/merchant-businesses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Merchant Businesses", description = "Merchant Businesses management endpoints")
public class MerchantBusinessResource {

        @Inject
        MerchantBusinessService merchantBusinessService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all merchant businesses")
        public Uni<Response> findAll(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantBusinessService.findAll(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active merchant businesses")
        public Uni<Response> findByActive(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantBusinessService.findByActive(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed merchant businesses")
        public Uni<Response> findByTrashed(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return merchantBusinessService.findByTrashed(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get merchant business by ID")
        public Uni<Response> findById(@PathParam("id") int id) {
                return merchantBusinessService.findById(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/create")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Create merchant business")
        public Uni<Response> create(pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest body) {
                return merchantBusinessService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @POST
        @Path("/update/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Update merchant business")
        public Uni<Response> update(@PathParam("id") int id,
                        pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest body) {
                return merchantBusinessService.update(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete merchant business")
        public Uni<Response> delete(@PathParam("id") int id) {
                return merchantBusinessService.delete(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore soft-deleted merchant business")
        public Uni<Response> restore(@PathParam("id") int id) {
                return merchantBusinessService.restore(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete merchant business")
        public Uni<Response> deletePermanent(@PathParam("id") int id) {
                return merchantBusinessService.deletePermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted merchant businesses")
        public Uni<Response> restoreAll() {
                return merchantBusinessService.restoreAll()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted merchant businesses")
        public Uni<Response> deleteAllPermanent() {
                return merchantBusinessService.deleteAllPermanent()
                                .map(dto -> Response.ok(dto).build());
        }
}
