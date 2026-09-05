package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.MerchantPolicyService;

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

@Path("/api/merchant-policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Merchant Policies", description = "Merchant Policies management endpoints")
public class MerchantPolicyResource {

    @Inject
    MerchantPolicyService merchantPolicyService;

    @GET
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List all merchant policies")
    public Uni<Response> findAll(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantPolicyService.findAll(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/active")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List active merchant policies")
    public Uni<Response> findByActive(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantPolicyService.findByActive(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/trashed")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "List trashed merchant policies")
    public Uni<Response> findByTrashed(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantPolicyService.findByTrashed(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Get merchant policy by ID")
    public Uni<Response> findById(@PathParam("id") int id) {
        return merchantPolicyService.findById(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/create")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Create merchant policy")
    public Uni<Response> create(pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest body) {
        return merchantPolicyService.create(body)
                .map(dto -> Response.status(Response.Status.CREATED)
                        .entity(dto)
                        .build());
    }

    @POST
    @Path("/update/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Update merchant policy")
    public Uni<Response> update(@PathParam("id") int id,
            pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest body) {
        return merchantPolicyService.update(id, body)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Soft-delete merchant policy")
    public Uni<Response> delete(@PathParam("id") int id) {
        return merchantPolicyService.delete(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore soft-deleted merchant policy")
    public Uni<Response> restore(@PathParam("id") int id) {
        return merchantPolicyService.restore(id)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/permanent/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete merchant policy")
    public Uni<Response> deletePermanent(@PathParam("id") int id) {
        return merchantPolicyService.deletePermanent(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore all soft-deleted merchant policies")
    public Uni<Response> restoreAll() {
        return merchantPolicyService.restoreAll()
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/permanent/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete all soft-deleted merchant policies")
    public Uni<Response> deleteAllPermanent() {
        return merchantPolicyService.deleteAllPermanent()
                .map(dto -> Response.ok(dto).build());
    }
}
