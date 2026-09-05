package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.MerchantDetailService;

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

@Path("/api/merchant-details")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Merchant Details", description = "Merchant Details management endpoints")
public class MerchantDetailResource {

    @Inject
    MerchantDetailService merchantDetailService;

    @Inject
    com.sanedge.gateway.service.FileService fileService;

    @POST
    @Path("/upload-cover")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Upload cover image")
    public Uni<Response> uploadCoverImage(
            @org.jboss.resteasy.reactive.RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file) {
        return Uni.createFrom().item(() -> {
            String filename = "static/merchant_cover/" + System.currentTimeMillis() + "_" + file.fileName();
            String savedPath = fileService.createFileImage(file, filename);
            if (savedPath == null) {
                throw new jakarta.ws.rs.WebApplicationException("Failed to upload cover image",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
            return Response.ok(java.util.Map.of("url", savedPath)).build();
        });
    }

    @POST
    @Path("/upload-logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Upload store logo")
    public Uni<Response> uploadLogo(
            @org.jboss.resteasy.reactive.RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file) {
        return Uni.createFrom().item(() -> {
            String filename = "static/merchant_logo/" + System.currentTimeMillis() + "_" + file.fileName();
            String savedPath = fileService.createFileImage(file, filename);
            if (savedPath == null) {
                throw new jakarta.ws.rs.WebApplicationException("Failed to upload logo image",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
            return Response.ok(java.util.Map.of("url", savedPath)).build();
        });
    }

    @GET
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List all merchant details")
    public Uni<Response> findAll(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantDetailService.findAll(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/active")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List active merchant details")
    public Uni<Response> findByActive(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantDetailService.findByActive(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/trashed")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "List trashed merchant details")
    public Uni<Response> findByTrashed(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return merchantDetailService.findByTrashed(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Get merchant detail by ID")
    public Uni<Response> findById(@PathParam("id") int id) {
        return merchantDetailService.findById(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/create")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Create merchant detail")
    public Uni<Response> create(pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest body) {
        return merchantDetailService.create(body)
                .map(dto -> Response.status(Response.Status.CREATED)
                        .entity(dto)
                        .build());
    }

    @POST
    @Path("/update/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Update merchant detail")
    public Uni<Response> update(@PathParam("id") int id,
            pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest body) {
        return merchantDetailService.update(id, body)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Soft-delete merchant detail")
    public Uni<Response> delete(@PathParam("id") int id) {
        return merchantDetailService.delete(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore soft-deleted merchant detail")
    public Uni<Response> restore(@PathParam("id") int id) {
        return merchantDetailService.restore(id)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/permanent/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete merchant detail")
    public Uni<Response> deletePermanent(@PathParam("id") int id) {
        return merchantDetailService.deletePermanent(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore all soft-deleted merchant details")
    public Uni<Response> restoreAll() {
        return merchantDetailService.restoreAll()
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/permanent/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete all soft-deleted merchant details")
    public Uni<Response> deleteAllPermanent() {
        return merchantDetailService.deleteAllPermanent()
                .map(dto -> Response.ok(dto).build());
    }
}
