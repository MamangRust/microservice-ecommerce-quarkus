package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.UserDto;
import io.smallrye.mutiny.Uni;

public interface UserService {
    Uni<UserDto.ApiResponsePaginationUser> listUsers(int page, int size, String search);
    Uni<UserDto.ApiResponsePaginationUserDeleteAt> getActiveUsers(int page, int size, String search);
    Uni<UserDto.ApiResponsePaginationUserDeleteAt> getTrashedUsers(int page, int size, String search);
    Uni<UserDto.ApiResponseUser> getUser(int id);
    Uni<UserDto.ApiResponseUser> createUser(UserDto.CreateRequest body);
    Uni<UserDto.ApiResponseUser> updateUser(int id, UserDto.UpdateRequest body);
    Uni<UserDto.ApiResponseUserDeleteAt> deleteUser(int id);
    Uni<UserDto.ApiResponseUserDeleteAt> restoreUser(int id);
    Uni<UserDto.SimpleResponse> deleteUserPermanent(int id);
    Uni<UserDto.SimpleResponse> restoreAllUser();
    Uni<UserDto.SimpleResponse> deleteAllUserPermanent();
}
