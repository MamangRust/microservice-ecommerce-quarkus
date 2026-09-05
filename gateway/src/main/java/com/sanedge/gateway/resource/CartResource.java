package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.CartService;

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

@Path("/api/carts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Carts", description = "Cart management endpoints")
public class CartResource {

        @Inject
        CartService cartService;

        @GET
        @Path("/user/{userId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "List all cart items for a user")
        public Uni<Response> findAll(
                        @PathParam("userId") int userId,
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return cartService.findAll(userId, page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Create or add item to cart")
        public Uni<Response> create(pb.cart.CartCommand.CreateCartRequest body) {
                return cartService.create(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @DELETE
        @Path("/{cartId}/user/{userId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Delete a cart item")
        public Uni<Response> delete(
                        @PathParam("cartId") int cartId,
                        @PathParam("userId") int userId) {
                return cartService.delete(cartId, userId)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/user/{userId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        @Operation(summary = "Clear all cart items for a user")
        public Uni<Response> deleteAll(
                        @PathParam("userId") int userId,
                        @QueryParam("cartIds") java.util.List<Integer> cartIds) {
                return cartService.deleteAll(userId, cartIds)
                                .map(dto -> Response.ok(dto).build());
        }
}
