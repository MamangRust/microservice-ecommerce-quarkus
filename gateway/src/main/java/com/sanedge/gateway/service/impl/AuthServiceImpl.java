package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.AuthDto;
import com.sanedge.gateway.service.AuthService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("auth")
    pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authService;

    @Override
    public Uni<AuthDto.RegisterResponse> register(AuthDto.RegisterRequest body) {
        return telemetryHelper.traceAndMetric("auth.register", () -> authService.registerUser(
                pb.Auth.RegisterRequest.newBuilder()
                        .setFirstname(body.firstname() == null ? "" : body.firstname())
                        .setLastname(body.lastname() == null ? "" : body.lastname())
                        .setEmail(body.email() == null ? "" : body.email())
                        .setPassword(body.password() == null ? "" : body.password())
                        .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                        .build())
                .map(AuthDto.RegisterResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to register user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.LoginResponse> login(AuthDto.LoginRequest body) {
        return telemetryHelper.traceAndMetric("auth.login", () -> authService.loginUser(
                pb.Auth.LoginRequest.newBuilder()
                        .setEmail(body.email())
                        .setPassword(body.password())
                        .build())
                .map(AuthDto.LoginResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to login user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.SimpleResponse> verify(AuthDto.VerifyCodeRequest body) {
        return telemetryHelper.traceAndMetric("auth.verify", () -> authService.verifyCode(
                pb.Auth.VerifyCodeRequest.newBuilder()
                        .setCode(body.code())
                        .build())
                .map(AuthDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to verify code: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.SimpleResponse> forgotPassword(AuthDto.ForgotPasswordRequest body) {
        return telemetryHelper.traceAndMetric("auth.forgotPassword", () -> authService.forgotPassword(
                pb.Auth.ForgotPasswordRequest.newBuilder()
                        .setEmail(body.email())
                        .build())
                .map(AuthDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed forgot password: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.SimpleResponse> resetPassword(AuthDto.ResetPasswordRequest body) {
        return telemetryHelper.traceAndMetric("auth.resetPassword", () -> authService.resetPassword(
                pb.Auth.ResetPasswordRequest.newBuilder()
                        .setResetToken(body.resetToken())
                        .setPassword(body.password())
                        .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                        .build())
                .map(AuthDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to reset password: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.RefreshTokenResponse> refresh(AuthDto.RefreshTokenRequest body) {
        return telemetryHelper.traceAndMetric("auth.refresh", () -> authService.refreshToken(
                pb.Auth.RefreshTokenRequest.newBuilder()
                        .setRefreshToken(body.refreshToken())
                        .build())
                .map(AuthDto.RefreshTokenResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to refresh token: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AuthDto.GetMeResponse> getMe(int userId) {
        return telemetryHelper.traceAndMetric("auth.getMe", () -> authService.getMe(
                pb.Auth.GetMeRequest.newBuilder()
                        .setUserId(userId)
                        .build())
                .map(AuthDto.GetMeResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed getMe for user " + userId + ": " + throwable.getMessage(), throwable)));
    }
}
