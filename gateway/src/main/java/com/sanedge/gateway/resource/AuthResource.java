package com.sanedge.gateway.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.sanedge.gateway.dto.AuthDto;
import com.sanedge.gateway.service.AuthService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @Operation(summary = "Register a new user")
    public Uni<Response> register(AuthDto.RegisterRequest body) {
        return authService.register(body)
                .map(dto -> Response.status(Response.Status.CREATED)
                        .entity(dto)
                        .build());
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login a user")
    public Uni<Response> login(AuthDto.LoginRequest body) {
        return authService.login(body)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/verify")
    @Operation(summary = "Verify user email by verification code")
    public Uni<Response> verify(AuthDto.VerifyCodeRequest body) {
        return authService.verify(body)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/forgot-password")
    @Operation(summary = "Initiate forgot password request")
    public Uni<Response> forgotPassword(AuthDto.ForgotPasswordRequest body) {
        return authService.forgotPassword(body)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/reset-password")
    @Operation(summary = "Reset user password")
    public Uni<Response> resetPassword(AuthDto.ResetPasswordRequest body) {
        return authService.resetPassword(body)
                .map(dto -> Response.ok(dto).build());
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh user access token")
    public Uni<Response> refresh(AuthDto.RefreshTokenRequest body) {
        return authService.refresh(body)
                .map(dto -> Response.ok(dto).build());
    }

    @GET
    @Path("/me")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    @Operation(summary = "Get current logged-in user profile")
    public Uni<Response> getMe(@QueryParam("userId") int userId) {
        return authService.getMe(userId)
                .map(dto -> Response.ok(dto).build());
    }
}
