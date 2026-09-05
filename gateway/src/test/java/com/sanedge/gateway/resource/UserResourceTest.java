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

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {
    @Mock
    UserService userService;
    private UserResource userResource;

    @BeforeEach
    void setUp() throws Exception {
        userResource = new UserResource();
        Field f = UserResource.class.getDeclaredField("userService");
        f.setAccessible(true);
        f.set(userResource, userService);
    }

    private UserDto.UserResponse mk(int id) {
        return new UserDto.UserResponse(id, "John", "Doe", "j@d.com", "", "");
    }

    @Test
    void listUsers_Success() {
        UserDto.ApiResponsePaginationUser dto = new UserDto.ApiResponsePaginationUser(
                List.of(mk(1)), "success", "ok");
        lenient().when(userService.listUsers(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.listUsers(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getActiveUsers_Success() {
        UserDto.ApiResponsePaginationUserDeleteAt dto = new UserDto.ApiResponsePaginationUserDeleteAt(
                List.of(), "success", "ok");
        lenient().when(userService.getActiveUsers(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.getActiveUsers(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getTrashedUsers_Success() {
        UserDto.ApiResponsePaginationUserDeleteAt dto = new UserDto.ApiResponsePaginationUserDeleteAt(
                List.of(), "success", "ok");
        lenient().when(userService.getTrashedUsers(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.getTrashedUsers(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getUser_Success() {
        UserDto.ApiResponseUser dto = new UserDto.ApiResponseUser(mk(1), "success", "ok");
        lenient().when(userService.getUser(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.getUser(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createUser_Success_Returns201() {
        UserDto.ApiResponseUser dto = new UserDto.ApiResponseUser(mk(1), "success", "ok");
        lenient().when(userService.createUser(any())).thenReturn(Uni.createFrom().item(dto));
        UserDto.CreateRequest req = new UserDto.CreateRequest("John", "Doe", "j@d.com", "p", "p");
        Response r = userResource.createUser(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateUser_Success() {
        UserDto.ApiResponseUser dto = new UserDto.ApiResponseUser(mk(1), "success", "ok");
        lenient().when(userService.updateUser(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        UserDto.UpdateRequest req = new UserDto.UpdateRequest("John", "Doe", "j@d.com", "p", "p");
        Response r = userResource.updateUser(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteUser_Success() {
        UserDto.ApiResponseUserDeleteAt dto = new UserDto.ApiResponseUserDeleteAt(mk(1), "success", "ok");
        lenient().when(userService.deleteUser(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.deleteUser(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreUser_Success() {
        UserDto.ApiResponseUserDeleteAt dto = new UserDto.ApiResponseUserDeleteAt(mk(1), "success", "ok");
        lenient().when(userService.restoreUser(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.restoreUser(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteUserPermanent_Success() {
        UserDto.SimpleResponse dto = new UserDto.SimpleResponse("success", "ok");
        lenient().when(userService.deleteUserPermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.deleteUserPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllUser_Success() {
        UserDto.SimpleResponse dto = new UserDto.SimpleResponse("success", "ok");
        lenient().when(userService.restoreAllUser()).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.restoreAllUser().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllUserPermanent_Success() {
        UserDto.SimpleResponse dto = new UserDto.SimpleResponse("success", "ok");
        lenient().when(userService.deleteAllUserPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = userResource.deleteAllUserPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
