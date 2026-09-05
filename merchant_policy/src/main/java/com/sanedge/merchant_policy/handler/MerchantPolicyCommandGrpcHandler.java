package com.sanedge.merchant_policy.handler;

import com.sanedge.merchant_policy.domain.requests.CreateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.requests.UpdateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.service.MerchantPolicyCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_policy.MutinyMerchantPolicyCommandServiceGrpc;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt;
import pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantPolicyCommandGrpcHandler
        extends MutinyMerchantPolicyCommandServiceGrpc.MerchantPolicyCommandServiceImplBase {

    @Inject
    MerchantPolicyCommandService merchantPolicyCommandService;

    @Override
    public Uni<ApiResponseMerchantPolicies> create(
            pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        CreateMerchantPolicyRequest domainReq = new CreateMerchantPolicyRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setPolicyType(request.getPolicyType());
        domainReq.setTitle(request.getTitle());
        domainReq.setDescription(request.getDescription());

        return merchantPolicyCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantPolicies.Builder builder = ApiResponseMerchantPolicies.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseMerchantPolicies> update(
            pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest request) {
        if (request.getMerchantPolicyId() <= 0) {
            return IdValidator.invalid("MerchantPolicy id");
        }
        UpdateMerchantPolicyRequest domainReq = new UpdateMerchantPolicyRequest();
        domainReq.setMerchantPolicyId(request.getMerchantPolicyId());
        domainReq.setPolicyType(request.getPolicyType());
        domainReq.setTitle(request.getTitle());
        domainReq.setDescription(request.getDescription());

        return merchantPolicyCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantPolicies.Builder builder = ApiResponseMerchantPolicies.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseMerchantPoliciesDeleteAt> trashedMerchantPolicies(FindByIdMerchantPoliciesRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantPolicyCommandService.trash((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantPoliciesDeleteAt.Builder builder = ApiResponseMerchantPoliciesDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseMerchantPoliciesDeleteAt> restoreMerchantPolicies(FindByIdMerchantPoliciesRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantPolicyCommandService.restore((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantPoliciesDeleteAt.Builder builder = ApiResponseMerchantPoliciesDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantDelete> deleteMerchantPoliciesPermanent(
            FindByIdMerchantPoliciesRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantPolicyCommandService.delete((long) request.getId())
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> restoreAllMerchantPolicies(
            com.google.protobuf.Empty request) {
        return merchantPolicyCommandService.restoreAll()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> deleteAllMerchantPoliciesPermanent(
            com.google.protobuf.Empty request) {
        return merchantPolicyCommandService.deleteAll()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse toProto(MerchantPoliciesResponse r) {
        if (r == null) {
            return pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse.getDefaultInstance();
        }
        pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse.Builder builder = pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPolicyType() != null) {
            builder.setPolicyType(r.getPolicyType());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt toProto(
            MerchantPoliciesResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt.Builder builder = pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPolicyType() != null) {
            builder.setPolicyType(r.getPolicyType());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
