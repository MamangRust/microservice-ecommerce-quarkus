package com.sanedge.auth.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.entity.AuthOutbox;
import com.sanedge.auth.repository.AuthOutboxRepository;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.role.MutinyRoleCommandServiceGrpc;
import pb.role.MutinyRoleQueryServiceGrpc;
import pb.role.RoleCommon.ApiResponseRole;
import pb.role.RoleCommon.AssignRoleToUserRequest;
import pb.role.RoleQuery.FindByIdUserRoleRequest;
import pb.role.RoleQuery.FindByNameRoleRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.user.UserCommandService;
import pb.user.UserCommon.FindByIdUserRequest;
import pb.user.UserCommon.UserResponse;
import pb.user.UserQuery.FindAllUserRequest;
import pb.user.UserQueryService;

@ApplicationScoped
public class AuthService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    @GrpcClient("user")
    UserQueryService userQueryService;

    @GrpcClient("user")
    UserCommandService userCommandService;

    @GrpcClient("role")
    MutinyRoleQueryServiceGrpc.MutinyRoleQueryServiceStub roleService;

    @GrpcClient("role")
    MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    ResetTokenRepository resetTokenRepository;

    @Inject
    RedisService redisService;

    @Inject
    KafkaService kafkaService;

    @Inject
    AuthOutboxRepository authOutboxRepository;

    @Inject
    JwtUtil jwtUtil;

    @Inject
    PasswordUtil passwordUtil;

    @Inject
    TracingMetrics tracingMetrics;

    @WithTransaction
    public Uni<UserResponse> register(RegisterRequest req) {
        String firstName = req.getFirstName();
        String lastName = req.getLastName();
        String email = req.getEmail();
        String password = req.getPassword();

        return tracingMetrics.traceAndMeasure("registerUser", "register", () -> {
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() > 0) {
                            for (UserResponse u : findAllResponse.getDataList()) {
                                if (u.getEmail().equalsIgnoreCase(email)) {
                                    return Uni.createFrom()
                                            .failure(new RuntimeException("User with this email already exists"));
                                }
                            }
                        }

                        CreateUserRequest createReq = CreateUserRequest.newBuilder()
                                .setFirstname(firstName)
                                .setLastname(lastName)
                                .setEmail(email)
                                .setPassword(password)
                                .setConfirmPassword(password)
                                .build();

                        return userCommandService.create(createReq);
                    })
                    .chain(createUserResponse -> {
                        if (!"success".equalsIgnoreCase(createUserResponse.getStatus())) {
                            return Uni.createFrom().failure(new RuntimeException(createUserResponse.getMessage()));
                        }

                        UserResponse user = createUserResponse.getData();
                        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                        return redisService.setWithExpirationReactive("verification:" + email, verificationCode, 900)
                                .chain(() -> redisService.setWithExpirationReactive(
                                        "verification_code:" + verificationCode,
                                        email, 900))
                                .chain(() -> sendWelcomeEmail(user, verificationCode))
                                .chain(() -> {
                                    return roleService
                                            .findByNameRole(
                                                    FindByNameRoleRequest.newBuilder().setName("ROLE_USER").build())
                                            .map(roleResp -> roleResp.hasData() ? roleResp.getData().getId() : 0)
                                            .onFailure().recoverWithItem(err -> {
                                                LOGGER.warn(
                                                        "Failed to resolve ROLE_USER during registration, skipping role assignment: {}",
                                                        err.getMessage());
                                                return 0;
                                            })
                                            .chain(roleId -> {
                                                if (roleId == null || roleId <= 0) {
                                                    // No role assigned; login's role lookup falls back to ROLE_USER.
                                                    return Uni.createFrom().item(user);
                                                }
                                                AssignRoleToUserRequest assignReq = AssignRoleToUserRequest
                                                        .newBuilder()
                                                        .setUserId(user.getId())
                                                        .setRoleId(roleId)
                                                        .build();
                                                return roleCommandService.assignRoleToUser(assignReq)
                                                        .replaceWith(user);
                                            });
                                });
                    });
        });
    }

    @WithTransaction
    public Uni<String[]> login(String email, String password) {
        String failedAttemptsKey = "failed_login:" + email;
        String lockKey = "account_locked:" + email;

        return tracingMetrics.traceAndMeasure("loginUser", "login", () -> {
            return redisService.existsReactive(lockKey)
                    .chain(locked -> {
                        if (locked) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Account is locked due to too many failed attempts"));
                        }
                        return userCommandService.verifyPassword(VerifyPasswordRequest.newBuilder()
                                .setEmail(email)
                                .setPassword(password)
                                .build());
                    })
                    .chain(verifyRes -> {
                        if (!verifyRes.getValid()) {
                            return handleFailedLogin(email, failedAttemptsKey, lockKey);
                        }

                        UserResponse user = verifyRes.getUser();

                        return fetchUserRoles(user.getId())
                                .chain(roles -> {
                                    String accessToken = jwtUtil.generateToken(user.getEmail(), roles,
                                            (long) user.getId());
                                    String refreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(),
                                            (long) user.getId());

                                    RefreshToken rt = new RefreshToken();
                                    rt.setUserId((long) user.getId());
                                    rt.setToken(refreshTokenStr);
                                    rt.setExpiration(new Timestamp(
                                            System.currentTimeMillis() + jwtUtil.getRefreshExpirationMs()));

                                    return redisService.deleteReactive(failedAttemptsKey)
                                            .chain(() -> refreshTokenRepository.deleteByUserId((long) user.getId()))
                                            .chain(() -> refreshTokenRepository.persist(rt))
                                            .map(v -> new String[] { accessToken, refreshTokenStr });
                                });
                    });
        });
    }

    @WithTransaction
    public Uni<String[]> refresh(String refreshTokenStr) {
        return tracingMetrics.traceAndMeasure("refreshToken", "refresh", () -> {
            if (!jwtUtil.validateToken(refreshTokenStr)) {
                return Uni.createFrom().failure(new RuntimeException("Invalid or expired refresh token"));
            }

            return refreshTokenRepository.findByToken(refreshTokenStr)
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Refresh token is invalid or expired"));
                        }

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    return fetchUserRoles(user.getId())
                                            .chain(roles -> {
                                                String newAccessToken = jwtUtil.generateToken(user.getEmail(), roles,
                                                        (long) user.getId());
                                                String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(),
                                                        (long) user.getId());

                                                rt.setToken(newRefreshTokenStr);
                                                rt.setExpiration(new Timestamp(System.currentTimeMillis()
                                                        + jwtUtil.getRefreshExpirationMs()));

                                                return refreshTokenRepository.persist(rt)
                                                        .map(v -> new String[] { newAccessToken, newRefreshTokenStr });
                                            });
                                });
                    });
        });
    }

    @WithTransaction
    public Uni<Void> forgotPassword(String email) {
        return tracingMetrics.traceAndMeasure("forgotPassword", "forgot_password", () -> {
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() == 0) {
                            return Uni.createFrom().failure(new RuntimeException("User not found"));
                        }

                        UserResponse user = findAllResponse.getData(0);
                        String token = UUID.randomUUID().toString();

                        ResetToken resetToken = new ResetToken();
                        resetToken.setUserId((long) user.getId());
                        resetToken.setToken(token);
                        resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000)); // 15 mins

                        return resetTokenRepository.deleteByUserId((long) user.getId())
                                .chain(() -> resetTokenRepository.persist(resetToken))
                                .chain(() -> sendForgotPasswordEmail(user, token));
                    });
        });
    }

    @WithTransaction
    public Uni<Void> resetPassword(ResetPasswordRequest req) {
        String token = req.getToken();
        String password = req.getPassword();
        String confirmPassword = req.getConfirmPassword();

        return tracingMetrics.traceAndMeasure("resetPassword", "reset_password", () -> {
            if (!password.equals(confirmPassword)) {
                return Uni.createFrom().failure(new RuntimeException("Passwords do not match"));
            }

            return resetTokenRepository.findByToken(token)
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom().failure(new RuntimeException("Invalid or expired reset token"));
                        }

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    UpdateUserRequest updateReq = UpdateUserRequest.newBuilder()
                                            .setId(user.getId())
                                            .setFirstname(user.getFirstname())
                                            .setLastname(user.getLastname())
                                            .setEmail(user.getEmail())
                                            .setPassword(password)
                                            .setConfirmPassword(confirmPassword)
                                            .build();

                                    return userCommandService.update(updateReq);
                                })
                                .chain(updateRes -> {
                                    if (!"success".equalsIgnoreCase(updateRes.getStatus())) {
                                        return Uni.createFrom().failure(new RuntimeException(updateRes.getMessage()));
                                    }
                                    return resetTokenRepository.delete(rt);
                                })
                                .replaceWithVoid();
                    });
        });
    }

    @WithTransaction
    public Uni<Void> logout(String refreshTokenStr) {
        return tracingMetrics.traceAndMeasure("logout", "logout",
                () -> refreshTokenRepository.deleteByToken(refreshTokenStr)
                        .replaceWithVoid());
    }

    public Uni<Void> verifyEmailByCode(String code) {
        return tracingMetrics.traceAndMeasure("verifyEmailByCode", "verify_email", () -> {
            String key = "verification_code:" + code;
            return redisService.getReactive(key)
                    .chain(email -> {
                        if (email == null) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Invalid or expired verification code"));
                        }
                        return redisService.deleteReactive(key)
                                .chain(() -> redisService.deleteReactive("verification:" + email))
                                .replaceWithVoid();
                    });
        });
    }

    public Uni<UserResponse> getMe(Long userId) {
        return tracingMetrics.traceAndMeasure("getMe", "get_me",
                () -> userQueryService.findById(FindByIdUserRequest.newBuilder().setId(userId.intValue()).build())
                        .map(res -> {
                            if (!"success".equalsIgnoreCase(res.getStatus()) || !res.hasData()) {
                                throw new RuntimeException("User not found");
                            }
                            return res.getData();
                        }));
    }

    /**
     * Loads the user's real role names from the role service so the JWT carries
     * actual roles (e.g. ROLE_ADMIN) instead of a hardcoded ROLE_USER.
     * Falls back to ROLE_USER when no roles are assigned or the role service is
     * unavailable, so login/refresh never break.
     */
    private Uni<List<String>> fetchUserRoles(int userId) {
        return roleService
                .findByUserId(FindByIdUserRoleRequest.newBuilder().setUserId(userId).build())
                .map(resp -> {
                    List<String> roles = new ArrayList<>();
                    if (resp != null) {
                        for (pb.role.RoleCommon.RoleResponse r : resp.getDataList()) {
                            String name = r.getName();
                            if (name != null && !name.isBlank()) {
                                roles.add(name);
                            }
                        }
                    }
                    return roles.isEmpty() ? Collections.singletonList("ROLE_USER") : roles;
                })
                .onFailure().recoverWithItem(err -> {
                    LOGGER.warn("Failed to load roles for user {}, falling back to ROLE_USER: {}",
                            userId, err.getMessage());
                    return Collections.singletonList("ROLE_USER");
                });
    }

    private Uni<String[]> handleFailedLogin(String email, String failedAttemptsKey, String lockKey) {
        return redisService.getReactive(failedAttemptsKey)
                .chain(attemptsStr -> {
                    int currentAttempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);
                    int newAttempts = currentAttempts + 1;
                    if (newAttempts >= 5) {
                        return redisService.setWithExpirationReactive(lockKey, "true", 3600) // lock 1 hr
                                .chain(() -> redisService.deleteReactive(failedAttemptsKey))
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Account is locked due to too many failed attempts")));
                    } else {
                        return redisService
                                .setWithExpirationReactive(failedAttemptsKey, String.valueOf(newAttempts), 600) // 10
                                                                                                                // mins
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Invalid credentials. Attempt " + newAttempts + " of 5")));
                    }
                });
    }

    private Uni<Void> sendWelcomeEmail(UserResponse user, String code) {
        String subject = "Welcome to Quarkus Modular Monolith";
        String body = String.format(
                "Hello %s %s,\n\nWelcome to our platform! Use the following code to verify your email address:\n\n%s\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), code);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return enqueueOutbox("email-service-topic-auth-register", user.getEmail(), payload);
    }

    private Uni<Void> sendForgotPasswordEmail(UserResponse user, String token) {
        String subject = "Reset Password Verification";
        String body = String.format(
                "Hello %s %s,\n\nYou have requested a password reset. Use the following token to reset your password:\n\n%s\n\nThis token will expire in 15 minutes.\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), token);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return enqueueOutbox("email-service-topic-auth-forgot-password", user.getEmail(), payload);
    }

    private Uni<Void> enqueueOutbox(String topic, String email, JsonObject payload) {
        String eventId = UUID.randomUUID().toString();
        JsonObject eventPayload = payload.copy()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", topic)
                .put("occurred_at", java.time.Instant.now().toString());

        if (authOutboxRepository == null) {
            return kafkaService.sendExistingEvent(topic, email, eventPayload);
        }

        AuthOutbox event = new AuthOutbox();
        event.setEventId(eventId);
        event.setTopic(topic);
        event.setEventKey(email);
        event.setPayload(eventPayload.encode());
        return authOutboxRepository.persist(event).replaceWithVoid();
    }
}