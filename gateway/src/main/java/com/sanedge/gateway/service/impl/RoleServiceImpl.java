package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RoleServiceImpl implements RoleService {

    private static final Logger LOG = Logger.getLogger(RoleServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("role")
    pb.role.MutinyRoleQueryServiceGrpc.MutinyRoleQueryServiceStub roleQueryService;

    @GrpcClient("role")
    pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    @Override
    public Uni<RoleDto.ApiResponsePaginationRole> listRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.listRoles", () -> roleQueryService.findAllRole(
                pb.role.RoleQuery.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(RoleDto.ApiResponsePaginationRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> getRole(int id) {
        return telemetryHelper.traceAndMetric("role.getRole", () -> roleQueryService.findByIdRole(
                pb.role.RoleCommon.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> getRoleByName(String name) {
        return telemetryHelper.traceAndMetric("role.getRoleByName", () -> roleQueryService.findByNameRole(
                pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                        .setName(name)
                        .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role by name " + name + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> getActiveRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.getActiveRoles", () -> roleQueryService.findByActive(
                pb.role.RoleQuery.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(RoleDto.ApiResponsePaginationRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> getTrashedRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.getTrashedRoles", () -> roleQueryService.findByTrashed(
                pb.role.RoleQuery.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(RoleDto.ApiResponsePaginationRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsesRole> getRolesByUserId(int userId) {
        return telemetryHelper.traceAndMetric("role.getRolesByUserId", () -> roleQueryService.findByUserId(
                pb.role.RoleQuery.FindByIdUserRoleRequest.newBuilder()
                        .setUserId(userId)
                        .build())
                .map(RoleDto.ApiResponsesRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get roles for user " + userId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> createRole(pb.role.RoleCommand.CreateRoleRequest req) {
        return telemetryHelper.traceAndMetric("role.createRole", () -> roleCommandService.createRole(req)
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> updateRole(int id, pb.role.RoleCommand.UpdateRoleRequest req) {
        pb.role.RoleCommand.UpdateRoleRequest body = pb.role.RoleCommand.UpdateRoleRequest.newBuilder(req)
                .setId(id)
                .build();
        return telemetryHelper.traceAndMetric("role.updateRole", () -> roleCommandService.updateRole(body)
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleDeleteAt> deleteRole(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRole", () -> roleCommandService.trashedRole(
                pb.role.RoleCommon.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(RoleDto.ApiResponseRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleDeleteAt> restoreRole(int id) {
        return telemetryHelper.traceAndMetric("role.restoreRole", () -> roleCommandService.restoreRole(
                pb.role.RoleCommon.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(RoleDto.ApiResponseRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.SimpleResponse> deleteRolePermanent(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRolePermanent", () -> roleCommandService.deleteRolePermanent(
                pb.role.RoleCommon.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(RoleDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.SimpleResponse> restoreAllRole() {
        return telemetryHelper.traceAndMetric("role.restoreAllRole", () -> roleCommandService.restoreAllRole(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(RoleDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.SimpleResponse> deleteAllRolePermanent() {
        return telemetryHelper.traceAndMetric("role.deleteAllRolePermanent", () -> roleCommandService.deleteAllRolePermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(RoleDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseUserRole> assignRoleToUser(pb.role.RoleCommon.AssignRoleToUserRequest req) {
        return telemetryHelper.traceAndMetric("role.assignRoleToUser", () -> roleCommandService.assignRoleToUser(req)
                .map(RoleDto.ApiResponseUserRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to assign role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<Void> removeRoleFromUser(pb.role.RoleCommon.RemoveRoleFromUserRequest req) {
        return telemetryHelper.traceAndMetric("role.removeRoleFromUser", () -> roleCommandService.removeRoleFromUser(req)
                .map(empty -> (Void) null)
                .onFailure().invoke(throwable -> LOG.error("Failed to remove role: " + throwable.getMessage(), throwable)));
    }
}
