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

import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.role.MutinyRoleQueryServiceGrpc.MutinyRoleQueryServiceStub roleQueryService;
    @Mock
    private pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    private RoleServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = RoleServiceImpl.class.getDeclaredField(name);
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
        service = new RoleServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("roleQueryService", roleQueryService);
        inject("roleCommandService", roleCommandService);
    }

    @Test
    void getRole_PropagatesRoleResponse() {
        pb.role.RoleCommon.ApiResponseRole proto = pb.role.RoleCommon.ApiResponseRole.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(roleQueryService.findByIdRole(any(pb.role.RoleCommon.FindByIdRoleRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.getRole(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createRole_PropagatesRoleResponse() {
        pb.role.RoleCommon.ApiResponseRole proto = pb.role.RoleCommon.ApiResponseRole.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.role.RoleCommand.CreateRoleRequest req = pb.role.RoleCommand.CreateRoleRequest.newBuilder().build();
        lenient().when(roleCommandService.createRole(any(pb.role.RoleCommand.CreateRoleRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.createRole(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteRole_TrashStub_PropagatesRoleDeleteAt() {
        pb.role.RoleCommon.ApiResponseRoleDeleteAt proto = pb.role.RoleCommon.ApiResponseRoleDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(roleCommandService.trashedRole(any(pb.role.RoleCommon.FindByIdRoleRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteRole(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void assignRoleToUser_PropagatesUserRoleResponse() {
        pb.role.RoleCommon.ApiResponseUserRole proto = pb.role.RoleCommon.ApiResponseUserRole.newBuilder()
                .setStatus("success").setMessage("assigned").build();
        pb.role.RoleCommon.AssignRoleToUserRequest req = pb.role.RoleCommon.AssignRoleToUserRequest.newBuilder().build();
        lenient().when(roleCommandService.assignRoleToUser(any(pb.role.RoleCommon.AssignRoleToUserRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.assignRoleToUser(req).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.role.RoleCommon.ApiResponseRoleDeleteAt proto = pb.role.RoleCommon.ApiResponseRoleDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(roleCommandService.restoreRole(any(pb.role.RoleCommon.FindByIdRoleRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restoreRole(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
