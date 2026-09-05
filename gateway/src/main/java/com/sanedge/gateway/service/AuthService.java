package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.AuthDto;
import io.smallrye.mutiny.Uni;

public interface AuthService {
    Uni<AuthDto.RegisterResponse> register(AuthDto.RegisterRequest body);
    Uni<AuthDto.LoginResponse> login(AuthDto.LoginRequest body);
    Uni<AuthDto.SimpleResponse> verify(AuthDto.VerifyCodeRequest body);
    Uni<AuthDto.SimpleResponse> forgotPassword(AuthDto.ForgotPasswordRequest body);
    Uni<AuthDto.SimpleResponse> resetPassword(AuthDto.ResetPasswordRequest body);
    Uni<AuthDto.RefreshTokenResponse> refresh(AuthDto.RefreshTokenRequest body);
    Uni<AuthDto.GetMeResponse> getMe(int userId);
}
