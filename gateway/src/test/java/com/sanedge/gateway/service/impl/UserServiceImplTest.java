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

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;
    @Mock
    private pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub userCommandService;

    private UserServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = UserServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new UserServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("userQueryService", userQueryService);
        inject("userCommandService", userCommandService);
    }

    @Test
    void getUser_PropagatesUserResponse() {
        pb.user.UserCommon.ApiResponseUser proto = pb.user.UserCommon.ApiResponseUser.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(userQueryService.findById(any(pb.user.UserCommon.FindByIdUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        UserDto.ApiResponseUser result = service.getUser(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createUser_PropagatesUserResponse() {
        pb.user.UserCommon.ApiResponseUser proto = pb.user.UserCommon.ApiResponseUser.newBuilder()
                .setStatus("success").setMessage("created").build();
        UserDto.CreateRequest req = new UserDto.CreateRequest("John", "Doe", "u@e.com", "p", "p");
        lenient().when(userCommandService.create(any(pb.user.UserCommand.CreateUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.createUser(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteUser_TrashStub_PropagatesUserDeleteAt() {
        pb.user.UserCommon.ApiResponseUserDeleteAt proto = pb.user.UserCommon.ApiResponseUserDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(userCommandService.trashedUser(any(pb.user.UserCommon.FindByIdUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteUser(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void deleteUserPermanent_PropagatesSimpleResponse() {
        pb.user.UserCommon.ApiResponseUserDelete proto = pb.user.UserCommon.ApiResponseUserDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(userCommandService.deleteUserPermanent(any(pb.user.UserCommon.FindByIdUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteUserPermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.user.UserCommon.ApiResponseUserDeleteAt proto = pb.user.UserCommon.ApiResponseUserDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(userCommandService.restoreUser(any(pb.user.UserCommon.FindByIdUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restoreUser(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
