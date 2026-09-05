package com.sanedge.gateway.dto;

import java.util.List;

public class UserDto {

    public record UserResponse(
            int id,
            String firstname,
            String lastname,
            String email,
            String createdAt,
            String updatedAt) {
        public static UserResponse from(pb.user.UserCommon.UserResponse proto) {
            return new UserResponse(
                    proto.getId(),
                    proto.getFirstname(),
                    proto.getLastname(),
                    proto.getEmail(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static UserResponse from(pb.user.UserCommon.UserResponseDeleteAt proto) {
            return new UserResponse(
                    proto.getId(),
                    proto.getFirstname(),
                    proto.getLastname(),
                    proto.getEmail(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllUserResponse(
            List<UserResponse> data,
            String status,
            String message) {
        public static FindAllUserResponse from(pb.user.UserCommon.ApiResponsePaginationUser proto) {
            return new FindAllUserResponse(
                    proto.getDataList().stream().map(UserResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllUserResponse from(pb.user.UserCommon.ApiResponsePaginationUserDeleteAt proto) {
            return new FindAllUserResponse(
                    proto.getDataList().stream().map(UserResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdUserResponse(
            UserResponse data,
            String status,
            String message) {
        public static FindByIdUserResponse from(pb.user.UserCommon.ApiResponseUser proto) {
            return new FindByIdUserResponse(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateUserRequest(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {}

    public record CreateUserResponse(
            UserResponse data,
            String status,
            String message) {
        public static CreateUserResponse from(pb.user.UserCommon.ApiResponseUser proto) {
            return new CreateUserResponse(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateUserRequest(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {}

    public record UpdateUserResponse(
            UserResponse data,
            String status,
            String message) {
        public static UpdateUserResponse from(pb.user.UserCommon.ApiResponseUser proto) {
            return new UpdateUserResponse(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedUserResponse(
            UserResponse data,
            String status,
            String message) {
        public static TrashedUserResponse from(pb.user.UserCommon.ApiResponseUserDeleteAt proto) {
            return new TrashedUserResponse(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateRequest(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {}

    public record UpdateRequest(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {}

    public record ApiResponsePaginationUser(
            List<UserResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationUser from(pb.user.UserCommon.ApiResponsePaginationUser proto) {
            return new ApiResponsePaginationUser(
                    proto.getDataList().stream().map(UserResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationUserDeleteAt(
            List<UserResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationUserDeleteAt from(pb.user.UserCommon.ApiResponsePaginationUserDeleteAt proto) {
            return new ApiResponsePaginationUserDeleteAt(
                    proto.getDataList().stream().map(UserResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseUser(
            UserResponse data,
            String status,
            String message) {
        public static ApiResponseUser from(pb.user.UserCommon.ApiResponseUser proto) {
            return new ApiResponseUser(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseUserDeleteAt(
            UserResponse data,
            String status,
            String message) {
        public static ApiResponseUserDeleteAt from(pb.user.UserCommon.ApiResponseUserDeleteAt proto) {
            return new ApiResponseUserDeleteAt(
                    proto.hasData() ? UserResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.user.UserCommon.ApiResponseUserDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.user.UserCommon.ApiResponseUserAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
