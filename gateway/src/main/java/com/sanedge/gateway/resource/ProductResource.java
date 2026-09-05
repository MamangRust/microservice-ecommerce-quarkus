package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.ProductService;

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

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products", description = "Product management endpoints")
public class ProductResource {

    @Inject
    ProductService productService;

    @Inject
    com.sanedge.gateway.service.FileService fileService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Upload product image")
    public Uni<Response> uploadImage(
            @org.jboss.resteasy.reactive.RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file) {
        return Uni.createFrom().item(() -> {
            String filename = "static/product/" + System.currentTimeMillis() + "_" + file.fileName();
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
    @Operation(summary = "List all products")
    public Uni<Response> findAll(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return productService.findAll(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/active")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "List active products")
    public Uni<Response> findByActive(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return productService.findByActive(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/trashed")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    @Operation(summary = "List trashed products")
    public Uni<Response> findByTrashed(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search) {
        return productService.findByTrashed(page, size, search)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Get product by ID")
    public Uni<Response> findById(@PathParam("id") int id) {
        return productService.findById(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/create")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Create a new product")
    public Uni<Response> createProduct(pb.product.ProductCommand.CreateProductRequest body) {
        return productService.create(body)
                .map(dto -> Response.status(Response.Status.CREATED)
                        .entity(dto)
                        .build());
    }

    @POST
    @Path("/update/{id}")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Update product")
    public Uni<Response> updateProduct(@PathParam("id") int id, pb.product.ProductCommand.UpdateProductRequest body) {
        return productService.update(id, body)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Soft-delete a product")
    public Uni<Response> deleteProduct(@PathParam("id") int id) {
        return productService.delete(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore a soft-deleted product")
    public Uni<Response> restoreProduct(@PathParam("id") int id) {
        return productService.restore(id)
                .map(dto -> Response.ok(dto).build());
    }

    @DELETE
    @Path("/permanent/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete a product")
    public Uni<Response> deleteProductPermanent(@PathParam("id") int id) {
        return productService.deletePermanent(id)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/restore/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Restore all soft-deleted products")
    public Uni<Response> restoreAllProducts() {
        return productService.restoreAll()
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/permanent/all")
    @RolesAllowed("ROLE_ADMIN")
    @Operation(summary = "Permanently delete all soft-deleted products")
    public Uni<Response> deleteAllProductsPermanent() {
        return productService.deleteAllPermanent()
                .map(dto -> Response.ok(dto).build());
    }
}
