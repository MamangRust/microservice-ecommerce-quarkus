package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.AuthDto;
import com.sanedge.gateway.service.AuthService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    private AuthResource authResource;

    @BeforeEach
    void setUp() throws Exception {
        authResource = new AuthResource();
        Field serviceField = AuthResource.class.getDeclaredField("authService");
        serviceField.setAccessible(true);
        serviceField.set(authResource, authService);
    }

    private AuthDto.SimpleResponse okSimple() {
        return new AuthDto.SimpleResponse("success", "ok");
    }

    @Test
    void register_Success_Returns201() {
        AuthDto.RegisterResponse dto = new AuthDto.RegisterResponse("success", "registered", null);
        lenient().when(authService.register(any(AuthDto.RegisterRequest.class)))
                .thenReturn(Uni.createFrom().item(dto));

        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest("John", "Doe", "u@e.com", "pwd", "pwd");
        Response response = authResource.register(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getEntity()).isEqualTo(dto);
    }

    @Test
    void register_Failure_PropagatesError() {
        lenient().when(authService.register(any(AuthDto.RegisterRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("User exists")));

        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest("John", "Doe", "u@e.com", "pwd", "pwd");

        assertThatThrownBy(() -> authResource.register(req).await().indefinitely())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User exists");
    }

    @Test
    void login_Success_Returns200() {
        AuthDto.LoginResponse dto = new AuthDto.LoginResponse("success", "logged in", null);
        lenient().when(authService.login(any(AuthDto.LoginRequest.class)))
                .thenReturn(Uni.createFrom().item(dto));

        AuthDto.LoginRequest req = new AuthDto.LoginRequest("u@e.com", "pwd");
        Response response = authResource.login(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(dto);
    }

    @Test
    void verify_Success_Returns200() {
        AuthDto.SimpleResponse dto = okSimple();
        lenient().when(authService.verify(any(AuthDto.VerifyCodeRequest.class)))
                .thenReturn(Uni.createFrom().item(dto));

        AuthDto.VerifyCodeRequest req = new AuthDto.VerifyCodeRequest("ABC123");
        Response response = authResource.verify(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(dto);
    }

    @Test
    void forgotPassword_Success_Returns200() {
        lenient().when(authService.forgotPassword(any(AuthDto.ForgotPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(okSimple()));

        AuthDto.ForgotPasswordRequest req = new AuthDto.ForgotPasswordRequest("u@e.com");
        Response response = authResource.forgotPassword(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void resetPassword_Success_Returns200() {
        lenient().when(authService.resetPassword(any(AuthDto.ResetPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(okSimple()));

        AuthDto.ResetPasswordRequest req = new AuthDto.ResetPasswordRequest("token", "newpwd", "newpwd");
        Response response = authResource.resetPassword(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void refresh_Success_Returns200() {
        AuthDto.RefreshTokenResponse dto = new AuthDto.RefreshTokenResponse("success", "refreshed", null);
        lenient().when(authService.refresh(any(AuthDto.RefreshTokenRequest.class)))
                .thenReturn(Uni.createFrom().item(dto));

        AuthDto.RefreshTokenRequest req = new AuthDto.RefreshTokenRequest("refresh");
        Response response = authResource.refresh(req).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(dto);
    }

    @Test
    void getMe_Success_Returns200() {
        AuthDto.GetMeResponse dto = new AuthDto.GetMeResponse("success", "me", null);
        lenient().when(authService.getMe(anyInt()))
                .thenReturn(Uni.createFrom().item(dto));

        Response response = authResource.getMe(1).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(dto);
    }

    @Test
    void getMe_Failure_PropagatesError() {
        lenient().when(authService.getMe(anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("User not found")));

        assertThatThrownBy(() -> authResource.getMe(999).await().indefinitely())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
