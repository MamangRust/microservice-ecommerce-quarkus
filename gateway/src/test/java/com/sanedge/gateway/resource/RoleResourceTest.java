package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class RoleResourceTest {
    @Mock
    RoleService roleService;
    private RoleResource roleResource;

    @BeforeEach
    void setUp() throws Exception {
        roleResource = new RoleResource();
        Field f = RoleResource.class.getDeclaredField("roleService");
        f.setAccessible(true);
        f.set(roleResource, roleService);
    }

    private RoleDto.RoleResponse mk(int id) {
        return new RoleDto.RoleResponse(id, "ROLE_USER", "", "");
    }

    private RoleDto.UserRoleResponse mkUserRole(int userId, int roleId) {
        return new RoleDto.UserRoleResponse(1, userId, roleId, "", "");
    }

    @Test
    void listRoles_Success() {
        RoleDto.ApiResponsePaginationRole dto = new RoleDto.ApiResponsePaginationRole(
                List.of(mk(1)), "success", "ok");
        lenient().when(roleService.listRoles(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.listRoles(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getRole_Success() {
        RoleDto.ApiResponseRole dto = new RoleDto.ApiResponseRole(mk(1), "success", "ok");
        lenient().when(roleService.getRole(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.getRole(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getRoleByName_Success() {
        RoleDto.ApiResponseRole dto = new RoleDto.ApiResponseRole(mk(1), "success", "ok");
        lenient().when(roleService.getRoleByName("ROLE_USER")).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.getRoleByName("ROLE_USER").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getActiveRoles_Success() {
        RoleDto.ApiResponsePaginationRoleDeleteAt dto = new RoleDto.ApiResponsePaginationRoleDeleteAt(
                List.of(), "success", "ok");
        lenient().when(roleService.getActiveRoles(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.getActiveRoles(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getTrashedRoles_Success() {
        RoleDto.ApiResponsePaginationRoleDeleteAt dto = new RoleDto.ApiResponsePaginationRoleDeleteAt(
                List.of(), "success", "ok");
        lenient().when(roleService.getTrashedRoles(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.getTrashedRoles(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getRolesByUserId_Success() {
        RoleDto.ApiResponsesRole dto = new RoleDto.ApiResponsesRole(List.of(mk(1)), "success", "ok");
        lenient().when(roleService.getRolesByUserId(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.getRolesByUserId(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createRole_Success_Returns201() {
        RoleDto.ApiResponseRole dto = new RoleDto.ApiResponseRole(mk(1), "success", "ok");
        lenient().when(roleService.createRole(any())).thenReturn(Uni.createFrom().item(dto));
        pb.role.RoleCommand.CreateRoleRequest req = pb.role.RoleCommand.CreateRoleRequest.newBuilder().setName("X").build();
        Response r = roleResource.createRole(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateRole_Success() {
        RoleDto.ApiResponseRole dto = new RoleDto.ApiResponseRole(mk(1), "success", "ok");
        lenient().when(roleService.updateRole(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.role.RoleCommand.UpdateRoleRequest req = pb.role.RoleCommand.UpdateRoleRequest.newBuilder().setName("X").build();
        Response r = roleResource.updateRole(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteRole_Success() {
        RoleDto.ApiResponseRoleDeleteAt dto = new RoleDto.ApiResponseRoleDeleteAt(mk(1), "success", "ok");
        lenient().when(roleService.deleteRole(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.deleteRole(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreRole_Success() {
        RoleDto.ApiResponseRoleDeleteAt dto = new RoleDto.ApiResponseRoleDeleteAt(mk(1), "success", "ok");
        lenient().when(roleService.restoreRole(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.restoreRole(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteRolePermanent_Success() {
        RoleDto.SimpleResponse dto = new RoleDto.SimpleResponse("success", "ok");
        lenient().when(roleService.deleteRolePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.deleteRolePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllRole_Success() {
        RoleDto.SimpleResponse dto = new RoleDto.SimpleResponse("success", "ok");
        lenient().when(roleService.restoreAllRole()).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.restoreAllRole().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllRolePermanent_Success() {
        RoleDto.SimpleResponse dto = new RoleDto.SimpleResponse("success", "ok");
        lenient().when(roleService.deleteAllRolePermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = roleResource.deleteAllRolePermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void assignRoleToUser_Success() {
        RoleDto.ApiResponseUserRole dto = new RoleDto.ApiResponseUserRole("success", "ok", mkUserRole(1, 2));
        lenient().when(roleService.assignRoleToUser(any())).thenReturn(Uni.createFrom().item(dto));
        pb.role.RoleCommon.AssignRoleToUserRequest req = pb.role.RoleCommon.AssignRoleToUserRequest.newBuilder().setUserId(1).setRoleId(2).build();
        Response r = roleResource.assignRoleToUser(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void removeRoleFromUser_Success_NoContent() {
        lenient().when(roleService.removeRoleFromUser(any())).thenReturn(Uni.createFrom().voidItem());
        pb.role.RoleCommon.RemoveRoleFromUserRequest req = pb.role.RoleCommon.RemoveRoleFromUserRequest.newBuilder().setUserId(1).setRoleId(2).build();
        Response r = roleResource.removeRoleFromUser(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(204);
    }
}
