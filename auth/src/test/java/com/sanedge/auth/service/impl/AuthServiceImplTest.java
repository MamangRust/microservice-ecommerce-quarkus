package com.sanedge.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.auth.entity.AuthOutbox;
import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.repository.AuthOutboxRepository;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.auth.service.AuthService;
import com.sanedge.auth.service.KafkaService;
import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.model.UserRole;
import com.sanedge.common.adapter.role.RolePort;
import com.sanedge.common.adapter.user.UserPort;
import com.sanedge.common.adapter.user_role.UserRolePort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

        @Mock
        UserPort userPort;

        @Mock
        RolePort rolePort;

        @Mock
        UserRolePort userRolePort;

        @Mock
        RefreshTokenRepository refreshTokenRepository;

        @Mock
        ResetTokenRepository resetTokenRepository;

        @Mock
        RedisService redisService;

        @Mock
        KafkaService kafkaService;

        @Mock
        AuthOutboxRepository authOutboxRepository;

        @Mock
        JwtUtil jwtUtil;

        @Mock
        PasswordUtil passwordUtil;

        @Mock
        TracingMetrics tracingMetrics;

        @InjectMocks
        AuthService authServiceUnderTest;

        private RegisterRequest registerReq;
        private User user;

        @BeforeEach
        void setUp() {
                registerReq = RegisterRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("john@example.com")
                                .password("SecurePass123!")
                                .build();

                user = new User(1, "John", "Doe", "john@example.com", "hashedPassword", null, null);

                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics)
                                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                lenient().when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(false));
                lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), any(Long.class)))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

                lenient().when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(kafkaService.sendExistingEvent(anyString(), anyString(), any(JsonObject.class)))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(authOutboxRepository.persist(any(AuthOutbox.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((AuthOutbox) inv.getArgument(0)));

                lenient().when(userRolePort.findByUserId(anyInt()))
                                .thenReturn(Uni.createFrom().item(List.of(new Role(1, "ROLE_USER", null, null))));

                lenient().when(passwordUtil.verifyPassword(anyString(), anyString())).thenReturn(true);

                lenient().when(jwtUtil.generateToken(anyString(), any(), any(Long.class)))
                                .thenReturn("access-token-john");
                lenient().when(jwtUtil.generateRefreshToken(anyString(), any(Long.class)))
                                .thenReturn("refresh-token-john");
                lenient().when(jwtUtil.validateToken(anyString())).thenReturn(true);
                lenient().when(jwtUtil.getRefreshExpirationMs()).thenReturn(3600000L);
        }

        @Test
        void registerUser_shouldSucceed() {
                when(userPort.findByEmail("john@example.com"))
                                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("User not found")));
                when(userPort.createUser(any(UserPort.RegisterData.class)))
                                .thenReturn(Uni.createFrom().item(user));
                when(rolePort.findByName("ROLE_USER"))
                                .thenReturn(Uni.createFrom().item(new Role(1, "ROLE_USER", null, null)));
                when(userRolePort.assignRoleToUser(anyInt(), anyInt()))
                                .thenReturn(Uni.createFrom().item(new UserRole(1, 1, 1, null, null)));

                User result = authServiceUnderTest.register(registerReq).await().indefinitely();

                assertThat(result).isNotNull();
                assertThat(result.email()).isEqualTo("john@example.com");
                assertThat(result.firstname()).isEqualTo("John");
                verify(userPort).createUser(any(UserPort.RegisterData.class));
                verify(authOutboxRepository).persist(any(AuthOutbox.class));
        }

        @Test
        void registerUser_shouldFail_whenEmailAlreadyExists() {
                when(userPort.findByEmail("john@example.com"))
                                .thenReturn(Uni.createFrom().item(user));

                try {
                        authServiceUnderTest.register(registerReq).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("already exists");
                }
        }

        @Test
        void login_shouldSucceed() {
                when(userPort.findByEmailAndVerify("john@example.com"))
                                .thenReturn(Uni.createFrom().item(user));

                lenient().when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
                lenient().when(refreshTokenRepository.persist(any(RefreshToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

                String[] tokens = authServiceUnderTest.login("john@example.com", "SecurePass123!").await()
                                .indefinitely();

                assertThat(tokens).hasSize(2);
                assertThat(tokens[0]).isEqualTo("access-token-john");
                assertThat(tokens[1]).isEqualTo("refresh-token-john");
        }

        @Test
        void login_shouldCarryRealRolesFromDatabase() {
                when(userPort.findByEmailAndVerify("john@example.com"))
                                .thenReturn(Uni.createFrom().item(user));

                when(userRolePort.findByUserId(1))
                                .thenReturn(Uni.createFrom().item(List.of(
                                                new Role(1, "ROLE_ADMIN", null, null),
                                                new Role(2, "ROLE_USER", null, null))));

                lenient().when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
                lenient().when(refreshTokenRepository.persist(any(RefreshToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

                String[] tokens = authServiceUnderTest.login("john@example.com", "SecurePass123!").await()
                                .indefinitely();

                assertThat(tokens).hasSize(2);
                verify(jwtUtil).generateToken(eq("john@example.com"), eq(List.of("ROLE_ADMIN", "ROLE_USER")),
                                eq(1L));
        }

        @Test
        void login_shouldFail_whenAccountLocked() {
                when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(true));

                try {
                        authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("locked");
                }
        }

        @Test
        void login_shouldFail_withInvalidCredentials() {
                when(userPort.findByEmailAndVerify("john@example.com"))
                                .thenReturn(Uni.createFrom().item(user));
                when(passwordUtil.verifyPassword("wrong", "hashedPassword")).thenReturn(false);

                try {
                        authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid credentials");
                }
        }

        @Test
        void refresh_shouldSucceed() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken("old-refresh-token");
                storedToken.setUserId(1L);
                storedToken.setExpiration(new Timestamp(System.currentTimeMillis() + 3600000));

                when(refreshTokenRepository.findByToken("old-refresh-token"))
                                .thenReturn(Uni.createFrom().item(storedToken));
                when(refreshTokenRepository.persist(any(RefreshToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

                when(userPort.findById(1)).thenReturn(Uni.createFrom().item(user));

                String[] tokens = authServiceUnderTest.refresh("old-refresh-token").await().indefinitely();

                assertThat(tokens).hasSize(2);
                assertThat(tokens[0]).isEqualTo("access-token-john");
                verify(refreshTokenRepository).persist(any(RefreshToken.class));
        }

        @Test
        void refresh_shouldFail_whenTokenInvalid() {
                when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

                try {
                        authServiceUnderTest.refresh("invalid-token").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid or expired");
                }
        }

        @Test
        void refresh_shouldFail_whenTokenNotFound() {
                when(refreshTokenRepository.findByToken("unknown-token"))
                                .thenReturn(Uni.createFrom().nullItem());

                try {
                        authServiceUnderTest.refresh("unknown-token").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("invalid or expired");
                }
        }

        @Test
        void forgotPassword_shouldSucceed() {
                when(userPort.findByEmail("john@example.com"))
                                .thenReturn(Uni.createFrom().item(user));

                lenient().when(resetTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
                lenient().when(resetTokenRepository.persist(any(ResetToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((ResetToken) inv.getArgument(0)));

                authServiceUnderTest.forgotPassword("john@example.com").await().indefinitely();

                verify(resetTokenRepository).persist(any(ResetToken.class));
                verify(authOutboxRepository).persist(any(AuthOutbox.class));
        }

        @Test
        void forgotPassword_shouldFail_whenUserNotFound() {
                when(userPort.findByEmail("unknown@example.com"))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("User not found: unknown@example.com")));

                try {
                        authServiceUnderTest.forgotPassword("unknown@example.com").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("User not found");
                }
        }

        @Test
        void resetPassword_shouldSucceed() {
                ResetToken resetToken = new ResetToken();
                resetToken.setToken("valid-reset-token");
                resetToken.setUserId(1L);
                resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000));

                when(resetTokenRepository.findByToken("valid-reset-token"))
                                .thenReturn(Uni.createFrom().item(resetToken));

                when(userPort.findById(1)).thenReturn(Uni.createFrom().item(user));
                when(userPort.updatePassword(1, "NewPass123!")).thenReturn(Uni.createFrom().item(user));

                lenient().when(resetTokenRepository.delete(any(ResetToken.class)))
                                .thenReturn(Uni.createFrom().voidItem());

                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("valid-reset-token")
                                .password("NewPass123!")
                                .confirmPassword("NewPass123!")
                                .build();

                authServiceUnderTest.resetPassword(req).await().indefinitely();

                verify(userPort).updatePassword(1, "NewPass123!");
        }

        @Test
        void resetPassword_shouldFail_whenPasswordsMismatch() {
                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("some-token")
                                .password("Pass1!")
                                .confirmPassword("Pass2!")
                                .build();

                try {
                        authServiceUnderTest.resetPassword(req).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("do not match");
                }
        }

        @Test
        void resetPassword_shouldFail_whenTokenExpired() {
                ResetToken expiredToken = new ResetToken();
                expiredToken.setToken("expired-token");
                expiredToken.setUserId(1L);
                expiredToken.setExpiration(new Timestamp(System.currentTimeMillis() - 3600000));

                when(resetTokenRepository.findByToken("expired-token"))
                                .thenReturn(Uni.createFrom().item(expiredToken));

                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("expired-token")
                                .password("NewPass123!")
                                .confirmPassword("NewPass123!")
                                .build();

                try {
                        authServiceUnderTest.resetPassword(req).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("expired");
                }
        }

        @Test
        void logout_shouldSucceed() {
                when(refreshTokenRepository.deleteByToken("refresh-token-to-revoke"))
                                .thenReturn(Uni.createFrom().item(1L));

                authServiceUnderTest.logout("refresh-token-to-revoke").await().indefinitely();

                verify(refreshTokenRepository).deleteByToken("refresh-token-to-revoke");
        }

        @Test
        void getMe_shouldReturnUser() {
                when(userPort.findById(1)).thenReturn(Uni.createFrom().item(user));

                User result = authServiceUnderTest.getMe(1L).await().indefinitely();

                assertThat(result).isNotNull();
                assertThat(result.id()).isEqualTo(1);
                assertThat(result.email()).isEqualTo("john@example.com");
        }

        @Test
        void getMe_shouldFail_whenUserNotFound() {
                when(userPort.findById(999))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("User not found: 999")));

                try {
                        authServiceUnderTest.getMe(999L).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("User not found");
                }
        }

        @Test
        void verifyEmail_shouldSucceed() {
                when(redisService.getReactive("verification_code:ABC123"))
                                .thenReturn(Uni.createFrom().item("john@example.com"));

                authServiceUnderTest.verifyEmailByCode("ABC123").await().indefinitely();

                verify(redisService).deleteReactive("verification_code:ABC123");
        }

        @Test
        void verifyEmail_shouldFail_whenInvalidCode() {
                when(redisService.getReactive("verification_code:INVALID"))
                                .thenReturn(Uni.createFrom().nullItem());

                try {
                        authServiceUnderTest.verifyEmailByCode("INVALID").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid or expired");
                }
        }

        /**
         * Finds the Supplier argument in the invocation regardless of whether it was
         * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
         * (arg index 3), then invokes it and returns the resulting Uni. This lets
         * a single Answer<?> body serve both traceAndMeasure overloads.
         */
        private Answer<Uni<?>> invokeSupplier() {
                return invocation -> {
                        Supplier<?> supplier = null;
                        for (Object arg : invocation.getArguments()) {
                                if (arg instanceof Supplier<?>) {
                                        supplier = (Supplier<?>) arg;
                                        break;
                                }
                        }
                        return supplier != null ? (Uni<?>) supplier.get() : null;
                };
        }
}
