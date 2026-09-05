package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.service.RoleService;

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

@Path("/api/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleResource {

        @Inject
        RoleService roleService;

        @GET
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List all roles")
        public Uni<Response> listRoles(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return roleService.listRoles(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/{id}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get role by ID")
        public Uni<Response> getRole(@PathParam("id") int id) {
                return roleService.getRole(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/name/{name}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get role by Name")
        public Uni<Response> getRoleByName(@PathParam("name") String name) {
                return roleService.getRoleByName(name)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/active")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "List active roles")
        public Uni<Response> getActiveRoles(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return roleService.getActiveRoles(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/trashed")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "List trashed roles")
        public Uni<Response> getTrashedRoles(
                        @QueryParam("page") @DefaultValue("1") int page,
                        @QueryParam("size") @DefaultValue("20") int size,
                        @QueryParam("search") String search) {
                return roleService.getTrashedRoles(page, size, search)
                                .map(dto -> Response.ok(dto).build());
        }

        @GET
        @Path("/user/{userId}")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        @Operation(summary = "Get roles by User ID")
        public Uni<Response> getRolesByUserId(@PathParam("userId") int userId) {
                return roleService.getRolesByUserId(userId)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Create a new role")
        public Uni<Response> createRole(pb.role.RoleCommand.CreateRoleRequest body) {
                return roleService.createRole(body)
                                .map(dto -> Response.status(Response.Status.CREATED)
                                                .entity(dto)
                                                .build());
        }

        @PUT
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Update role")
        public Uni<Response> updateRole(@PathParam("id") int id, pb.role.RoleCommand.UpdateRoleRequest body) {
                return roleService.updateRole(id, body)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Soft-delete a role")
        public Uni<Response> deleteRole(@PathParam("id") int id) {
                return roleService.deleteRole(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore a soft-deleted role")
        public Uni<Response> restoreRole(@PathParam("id") int id) {
                return roleService.restoreRole(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @DELETE
        @Path("/permanent/{id}")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete a role")
        public Uni<Response> deleteRolePermanent(@PathParam("id") int id) {
                return roleService.deleteRolePermanent(id)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/restore/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Restore all soft-deleted roles")
        public Uni<Response> restoreAllRole() {
                return roleService.restoreAllRole()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/permanent/all")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Permanently delete all soft-deleted roles")
        public Uni<Response> deleteAllRolePermanent() {
                return roleService.deleteAllRolePermanent()
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/assign")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Assign a role to a user")
        public Uni<Response> assignRoleToUser(pb.role.RoleCommon.AssignRoleToUserRequest body) {
                return roleService.assignRoleToUser(body)
                                .map(dto -> Response.ok(dto).build());
        }

        @POST
        @Path("/remove")
        @RolesAllowed("ROLE_ADMIN")
        @Operation(summary = "Remove a role from a user")
        public Uni<Response> removeRoleFromUser(pb.role.RoleCommon.RemoveRoleFromUserRequest body) {
                return roleService.removeRoleFromUser(body)
                                .map(ignored -> Response.noContent().build());
        }
}
