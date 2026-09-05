package com.sanedge.gateway.resource;

import jakarta.annotation.security.RolesAllowed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.SliderService;

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

@Path("/api/sliders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sliders", description = "Slider management endpoints")
public class SliderResource {

    @Inject
    SliderService sliderService;

    @Inject
    com.sanedge.gateway.service.FileService fileService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "Upload slider image")
    public Uni<Response> uploadImage(
            @org.jboss.resteasy.reactive.RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file) {
        return Uni.createFrom().item(() -> {
            String filename = "static/slider/" + System.currentTimeMillis() + "_" + file.fileName();
            String savedPath = fileService.createFileImage(file, filename);
            if (savedPath == null) {
                throw new jakarta.ws.rs.WebApplicationException("Failed to upload image",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
            return Response.ok(java.util.Map.of("url", savedPath)).build();
        });
    }

    @GET
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List all sliders")
    public Uni<Response> findAll(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return sliderService.findAll(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/active")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List active sliders")
    public Uni<Response> findByActive(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return sliderService.findByActive(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/trashed")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "List trashed sliders")
    public Uni<Response> findByTrashed(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return sliderService.findByTrashed(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Get slider by ID")
    public Uni<Response> findById(@PathParam("id") int id) {
        return sliderService.findById(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/create")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "Create a new slider")
    public Uni<Response> createSlider(pb.slider.SliderCommand.CreateSliderRequest body) {
        return sliderService.create(body)
                .map(dto -> Response.status(Response.Status.CREATED)
                        .entity(dto)
                        .build());
    }

    @POST
    @Path("/update/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "Update slider")
    public Uni<Response> updateSlider(@PathParam("id") int id, pb.slider.SliderCommand.UpdateSliderRequest body) {
        return sliderService.update(id, body)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Soft-delete a slider")
    public Uni<Response> deleteSlider(@PathParam("id") int id) {
        return sliderService.delete(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore a soft-deleted slider")
    public Uni<Response> restoreSlider(@PathParam("id") int id) {
        return sliderService.restore(id)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/permanent/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete a slider")
    public Uni<Response> deleteSliderPermanent(@PathParam("id") int id) {
        return sliderService.deletePermanent(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore all soft-deleted sliders")
    public Uni<Response> restoreAllSliders() {
        return sliderService.restoreAll()
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/permanent/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete all soft-deleted sliders")
    public Uni<Response> deleteAllSlidersPermanent() {
        return sliderService.deleteAllPermanent()
                .map(dto -> Response.ok(dto).build());
    }
}
