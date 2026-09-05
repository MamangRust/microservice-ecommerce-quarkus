package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.ShippingAddressService;

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

@Path("/api/shipping-addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Shipping Addresses", description = "Shipping Address management endpoints")
public class ShippingAddressResource {

        @Inject
        ShippingAddressService shippingAddressService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all shipping addresses")
        public Uni<Response> findAll(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return shippingAddressService.findAll(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/order/{orderId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get shipping address by Order ID")
        public Uni<Response> findByOrder(@PathParam("orderId") int orderId) {
                return shippingAddressService.findByOrder(orderId)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List active shipping addresses")
        public Uni<Response> findByActive(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return shippingAddressService.findByActive(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List trashed shipping addresses")
        public Uni<Response> findByTrashed(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return shippingAddressService.findByTrashed(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Get shipping address by ID")
        public Uni<Response> findById(@PathParam("id") int id) {
                return shippingAddressService.findById(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/create")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Create shipping address")
        public Uni<Response> create(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest body) {
                return shippingAddressService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @POST
        @Path("/update/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Update shipping address")
        public Uni<Response> update(@PathParam("id") int id,
                        pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest body) {
                return shippingAddressService.update(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete shipping address")
        public Uni<Response> delete(@PathParam("id") int id) {
                return shippingAddressService.delete(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore soft-deleted shipping address")
        public Uni<Response> restore(@PathParam("id") int id) {
                return shippingAddressService.restore(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete shipping address")
        public Uni<Response> deletePermanent(@PathParam("id") int id) {
                return shippingAddressService.deletePermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted shipping addresses")
        public Uni<Response> restoreAll() {
                return shippingAddressService.restoreAll()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted shipping addresses")
        public Uni<Response> deleteAllPermanent() {
                return shippingAddressService.deleteAllPermanent()
                                .map(dto -> Response.ok(dto).build());
        }
}
