package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub;
import pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserIsVerifiedRequest;
import pb.user.UserCommand.UpdateUserPasswordRequest;
import pb.user.UserCommon.ApiResponseUser;
import pb.user.UserCommon.FindByEmailRequest;
import pb.user.UserCommon.FindByIdUserRequest;
import pb.user.UserCommon.FindByVerificationCodeRequest;
import pb.user.UserCommon.UserResponse;
import pb.user.UserCommon.UserResponseWithPassword;

@ApplicationScoped
public class UserAdapter implements UserPort {

    @GrpcClient("user")
    MutinyUserQueryServiceStub query;

    @GrpcClient("user")
    MutinyUserCommandServiceStub command;

    @Override
    public Uni<User> findById(int id) {
        return query.findById(FindByIdUserRequest.newBuilder().setId(id).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("User not found: " + id);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<User> findByEmail(String email) {
        return query.findByEmail(FindByEmailRequest.newBuilder().setEmail(email).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("User not found: " + email);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<User> findByEmailAndVerify(String email) {
        return findByEmail(email);
    }

    @Override
    public Uni<User> findByVerificationCode(String code) {
        return query.findByVerificationCode(FindByVerificationCodeRequest.newBuilder()
                .setVerificationCode(code)
                .build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("User not found for verification code");
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<User> createUser(RegisterData data) {
        return command.create(CreateUserRequest.newBuilder()
                .setFirstname(data.firstname())
                .setLastname(data.lastname())
                .setEmail(data.email())
                .setPassword(data.password())
                .setConfirmPassword(data.confirmPassword())
                .build())
                .map(resp -> requireData(resp, "Failed to create user"));
    }

    @Override
    public Uni<User> updateIsVerified(int userId, boolean isVerified) {
        return command.updateIsVerified(UpdateUserIsVerifiedRequest.newBuilder()
                .setId(userId)
                .setIsVerified(isVerified)
                .build())
                .map(resp -> requireData(resp, "Failed to update user verification: " + userId));
    }

    @Override
    public Uni<User> updatePassword(int userId, String password) {
        return command.updatePassword(UpdateUserPasswordRequest.newBuilder()
                .setId(userId)
                .setPassword(password)
                .build())
                .map(resp -> requireData(resp, "Failed to update user password: " + userId));
    }

    @Override
    public Uni<Void> deleteUserPermanent(int userId) {
        // Best effort trash dulu (user yang sudah trashed mengembalikan error, abaikan),
        // lalu purge — meniru DeleteUserPermanent di adapter Go.
        return command.trashedUser(FindByIdUserRequest.newBuilder().setId(userId).build())
                .onFailure().recoverWithItem(() -> null)
                .chain(ignored -> command.deleteUserPermanent(
                        FindByIdUserRequest.newBuilder().setId(userId).build()))
                .replaceWithVoid();
    }

    private static User requireData(ApiResponseUser resp, String message) {
        if (resp == null || !resp.hasData()) {
            throw new AdapterException(message);
        }
        return toModel(resp.getData());
    }

    static User toModel(UserResponse u) {
        if (u == null) {
            return null;
        }
        return new User(
                u.getId(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                null,
                ProtoTime.parse(u.getCreatedAt()),
                ProtoTime.parse(u.getUpdatedAt()));
    }

    static User toModel(UserResponseWithPassword u) {
        if (u == null) {
            return null;
        }
        return new User(
                u.getId(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                u.getPassword(),
                ProtoTime.parse(u.getCreatedAt()),
                ProtoTime.parse(u.getUpdatedAt()));
    }
}
