package com.sanedge.gateway.dto;

import java.util.List;

public class RoleDto {

    public record RoleResponse(
            int id,
            String name,
            String createdAt,
            String updatedAt) {
        public static RoleResponse from(pb.role.RoleCommon.RoleResponse proto) {
            return new RoleResponse(proto.getId(), proto.getName(), proto.getCreatedAt(), proto.getUpdatedAt());
        }
        public static RoleResponse from(pb.role.RoleCommon.RoleResponseDeleteAt proto) {
            return new RoleResponse(proto.getId(), proto.getName(), proto.getCreatedAt(), proto.getUpdatedAt());
        }
    }

    public record FindAllRoleResponse(
            List<RoleResponse> data,
            String status,
            String message) {
        public static FindAllRoleResponse from(pb.role.RoleCommon.ApiResponsePaginationRole proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllRoleResponse from(pb.role.RoleCommon.ApiResponsePaginationRoleDeleteAt proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllRoleResponse from(pb.role.RoleCommon.ApiResponsesRole proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static FindByIdRoleResponse from(pb.role.RoleCommon.ApiResponseRole proto) {
            return new FindByIdRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdRoleResponse from(pb.role.RoleCommon.ApiResponseRoleDeleteAt proto) {
            return new FindByIdRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateRoleRequest(
            String name) {}

    public record CreateRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static CreateRoleResponse from(pb.role.RoleCommon.ApiResponseRole proto) {
            return new CreateRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateRoleRequest(
            String name) {}

    public record UpdateRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static UpdateRoleResponse from(pb.role.RoleCommon.ApiResponseRole proto) {
            return new UpdateRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static TrashedRoleResponse from(pb.role.RoleCommon.ApiResponseRoleDeleteAt proto) {
            return new TrashedRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.role.RoleCommon.ApiResponseRoleDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.role.RoleCommon.ApiResponseRoleAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record UserRoleResponse(
            int userRoleId,
            int userId,
            int roleId,
            String createdAt,
            String updatedAt) {
        public static UserRoleResponse from(pb.role.RoleCommon.UserRoleResponse proto) {
            return new UserRoleResponse(
                    proto.getUserRoleId(),
                    proto.getUserId(),
                    proto.getRoleId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record AssignRoleToUserResponse(
            String status,
            String message,
            UserRoleResponse data) {
        public static AssignRoleToUserResponse from(pb.role.RoleCommon.ApiResponseUserRole proto) {
            return new AssignRoleToUserResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? UserRoleResponse.from(proto.getData()) : null
            );
        }
    }

    public record ApiResponsePaginationRole(
            List<RoleResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationRole from(pb.role.RoleCommon.ApiResponsePaginationRole proto) {
            return new ApiResponsePaginationRole(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationRole from(pb.role.RoleCommon.ApiResponsePaginationRoleDeleteAt proto) {
            return new ApiResponsePaginationRole(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseRole(
            RoleResponse data,
            String status,
            String message) {
        public static ApiResponseRole from(pb.role.RoleCommon.ApiResponseRole proto) {
            return new ApiResponseRole(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseRole from(pb.role.RoleCommon.ApiResponseRoleDeleteAt proto) {
            return new ApiResponseRole(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationRoleDeleteAt(
            List<RoleResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationRoleDeleteAt from(pb.role.RoleCommon.ApiResponsePaginationRoleDeleteAt proto) {
            return new ApiResponsePaginationRoleDeleteAt(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsesRole(
            List<RoleResponse> data,
            String status,
            String message) {
        public static ApiResponsesRole from(pb.role.RoleCommon.ApiResponsesRole proto) {
            return new ApiResponsesRole(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseRoleDeleteAt(
            RoleResponse data,
            String status,
            String message) {
        public static ApiResponseRoleDeleteAt from(pb.role.RoleCommon.ApiResponseRoleDeleteAt proto) {
            return new ApiResponseRoleDeleteAt(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.role.RoleCommon.ApiResponseRoleDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.role.RoleCommon.ApiResponseRoleAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponseUserRole(
            String status,
            String message,
            UserRoleResponse data) {
        public static ApiResponseUserRole from(pb.role.RoleCommon.ApiResponseUserRole proto) {
            return new ApiResponseUserRole(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? UserRoleResponse.from(proto.getData()) : null
            );
        }
    }
}
