package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;

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

@Path("/api/category")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Category", description = "Category management and statistics endpoints")
public class CategoryResource {

        @Inject
        CategoryService categoryService;

        @Inject
        com.sanedge.gateway.service.FileService fileService;

        @POST
        @Path("/upload")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Upload category image")
        public Uni<Response> uploadImage(
                        @org.jboss.resteasy.reactive.RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file) {
                return Uni.createFrom().item(() -> {
                        String filename = "static/category/" + System.currentTimeMillis() + "_" + file.fileName();
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
        @Operation(summary = "List all categories")
        public Uni<Response> findAll(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return categoryService.findAll(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active categories")
        public Uni<Response> findByActive(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return categoryService.findByActive(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed categories")
        public Uni<Response> findByTrashed(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return categoryService.findByTrashed(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get category by ID")
        public Uni<Response> findById(@PathParam("id") int id) {
                return categoryService.findById(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/monthly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly total pricing stats")
        public Uni<Response> findMonthTotalPrice(
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return categoryService.findMonthTotalPrice(year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/yearly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly total pricing stats")
        public Uni<Response> findYearTotalPrice(@QueryParam("year") int year) {
                return categoryService.findYearTotalPrice(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/merchant/monthly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly total pricing stats by merchant")
        public Uni<Response> findMonthTotalPriceByMerchant(
                        @QueryParam("merchantId") int merchantId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return categoryService.findMonthTotalPriceByMerchant(merchantId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/merchant/yearly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly total pricing stats by merchant")
        public Uni<Response> findYearTotalPriceByMerchant(
                        @QueryParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return categoryService.findYearlyTotalPricesByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/mycategory/monthly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly total pricing stats by category ID")
        public Uni<Response> findMonthTotalPriceById(
                        @QueryParam("categoryId") int categoryId,
                        @QueryParam("year") int year,
                        @QueryParam("month") int month) {
                return categoryService.findMonthlyTotalPricesById(categoryId, year, month)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/mycategory/yearly-total-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly total pricing stats by category ID")
        public Uni<Response> findYearTotalPriceById(
                        @QueryParam("categoryId") int categoryId,
                        @QueryParam("year") int year) {
                return categoryService.findYearlyTotalPricesById(categoryId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/monthly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly pricing stats")
        public Uni<Response> findMonthPrice(@QueryParam("year") int year) {
                return categoryService.findMonthPrice(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/yearly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly pricing stats")
        public Uni<Response> findYearPrice(@QueryParam("year") int year) {
                return categoryService.findYearPrice(year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/merchant/monthly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly pricing stats by merchant")
        public Uni<Response> findMonthPriceByMerchant(
                        @QueryParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return categoryService.findMonthPriceByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/merchant/yearly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly pricing stats by merchant")
        public Uni<Response> findYearPriceByMerchant(
                        @QueryParam("merchantId") int merchantId,
                        @QueryParam("year") int year) {
                return categoryService.findYearPriceByMerchant(merchantId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/mycategory/monthly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get monthly pricing stats by category ID")
        public Uni<Response> findMonthPriceById(
                        @QueryParam("categoryId") int categoryId,
                        @QueryParam("year") int year) {
                return categoryService.findMonthPriceById(categoryId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/mycategory/yearly-pricing")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get yearly pricing stats by category ID")
        public Uni<Response> findYearPriceById(
                        @QueryParam("categoryId") int categoryId,
                        @QueryParam("year") int year) {
                return categoryService.findYearPriceById(categoryId, year)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/create")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Create a new category")
        public Uni<Response> createCategory(CategoryDto.CreateCategoryRequest body) {
                return categoryService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @POST
        @Path("/update/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Update category")
        public Uni<Response> updateCategory(@PathParam("id") int id,
                        CategoryDto.UpdateCategoryRequest body) {
                return categoryService.update(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/trashed/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete category")
        public Uni<Response> trashedCategory(@PathParam("id") int id) {
                return categoryService.trash(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore soft-deleted category")
        public Uni<Response> restoreCategory(@PathParam("id") int id) {
                return categoryService.restore(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete category")
        public Uni<Response> deleteCategoryPermanent(@PathParam("id") int id) {
                return categoryService.deleteCategoryPermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted categories")
        public Uni<Response> restoreAllCategories() {
                return categoryService.restoreAllCategories()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted categories")
        public Uni<Response> deleteAllCategoriesPermanent() {
                return categoryService.deleteAllCategoriesPermanent()
                                .map(dto -> Response.ok(dto).build());
        }
}
