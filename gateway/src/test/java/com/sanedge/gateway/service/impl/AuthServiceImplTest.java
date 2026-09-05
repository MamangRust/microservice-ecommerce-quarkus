package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.AuthDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authStub;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() throws Exception {

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        authService = new AuthServiceImpl();
        Field telemetryField = AuthServiceImpl.class.getDeclaredField("telemetryHelper");
        telemetryField.setAccessible(true);
        telemetryField.set(authService, telemetryHelper);
        Field stubField = AuthServiceImpl.class.getDeclaredField("authService");
        stubField.setAccessible(true);
        stubField.set(authService, authStub);
    }

    @Test
    void register_PropagatesRegisterResponse() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest("John", "Doe", "u@e.com", "pwd", "pwd");
        pb.Auth.ApiResponseRegister response = pb.Auth.ApiResponseRegister.newBuilder()
                .setStatus("success")
                .setMessage("registered")
                .build();
        lenient().when(authStub.registerUser(any(pb.Auth.RegisterRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.RegisterResponse result = authService.register(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("registered");
        assertThat(result.data()).isNull();
    }

    @Test
    void login_PropagatesLoginResponse() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest("u@e.com", "pwd");
        pb.Auth.ApiResponseLogin response = pb.Auth.ApiResponseLogin.newBuilder()
                .setStatus("success")
                .setMessage("logged in")
                .build();
        lenient().when(authStub.loginUser(any(pb.Auth.LoginRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.LoginResponse result = authService.login(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("logged in");
    }

    @Test
    void verify_PropagatesVerifyCodeResponse() {
        AuthDto.VerifyCodeRequest req = new AuthDto.VerifyCodeRequest("ABC123");
        pb.Auth.ApiResponseVerifyCode response = pb.Auth.ApiResponseVerifyCode.newBuilder()
                .setStatus("success")
                .setMessage("verified")
                .build();
        lenient().when(authStub.verifyCode(any(pb.Auth.VerifyCodeRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.SimpleResponse result = authService.verify(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("verified");
    }

    @Test
    void forgotPassword_PropagatesForgotPasswordResponse() {
        AuthDto.ForgotPasswordRequest req = new AuthDto.ForgotPasswordRequest("u@e.com");
        pb.Auth.ApiResponseForgotPassword response = pb.Auth.ApiResponseForgotPassword.newBuilder()
                .setStatus("success")
                .setMessage("email sent")
                .build();
        lenient().when(authStub.forgotPassword(any(pb.Auth.ForgotPasswordRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.SimpleResponse result = authService.forgotPassword(req).await().indefinitely();

        assertThat(result.message()).isEqualTo("email sent");
    }

    @Test
    void resetPassword_PropagatesResetPasswordResponse() {
        AuthDto.ResetPasswordRequest req = new AuthDto.ResetPasswordRequest("token", "newpwd", "newpwd");
        pb.Auth.ApiResponseResetPassword response = pb.Auth.ApiResponseResetPassword.newBuilder()
                .setStatus("success")
                .setMessage("password reset")
                .build();
        lenient().when(authStub.resetPassword(any(pb.Auth.ResetPasswordRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.SimpleResponse result = authService.resetPassword(req).await().indefinitely();

        assertThat(result.message()).isEqualTo("password reset");
    }

    @Test
    void refresh_PropagatesRefreshTokenResponse() {
        AuthDto.RefreshTokenRequest req = new AuthDto.RefreshTokenRequest("refresh");
        pb.Auth.ApiResponseRefreshToken response = pb.Auth.ApiResponseRefreshToken.newBuilder()
                .setStatus("success")
                .setMessage("refreshed")
                .build();
        lenient().when(authStub.refreshToken(any(pb.Auth.RefreshTokenRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.RefreshTokenResponse result = authService.refresh(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMe_PropagatesGetMeResponse() {
        pb.Auth.ApiResponseGetMe response = pb.Auth.ApiResponseGetMe.newBuilder()
                .setStatus("success")
                .setMessage("me")
                .build();
        lenient().when(authStub.getMe(any(pb.Auth.GetMeRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(response));

        AuthDto.GetMeResponse result = authService.getMe(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNull();
    }
}
