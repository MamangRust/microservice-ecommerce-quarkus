package com.sanedge.merchant_business.handler;

import com.sanedge.merchant_business.domain.requests.CreateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.requests.UpdateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.service.MerchantBusinessCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_business.MutinyMerchantBusinessCommandServiceGrpc;
import pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness;
import pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt;
import pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantBusinessCommandGrpcHandler
        extends MutinyMerchantBusinessCommandServiceGrpc.MerchantBusinessCommandServiceImplBase {

    @Inject
    MerchantBusinessCommandService merchantBusinessCommandService;

    @Override
    public Uni<ApiResponseMerchantBusiness> create(
            pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }        CreateMerchantBusinessRequest domainReq = new CreateMerchantBusinessRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setBusinessType(request.getBusinessType());
        domainReq.setTaxId(request.getTaxId());
        domainReq.setEstablishedYear(request.getEstablishedYear());
        domainReq.setNumberOfEmployees(request.getNumberOfEmployees());
        domainReq.setWebsiteUrl(request.getWebsiteUrl());

        return merchantBusinessCommandService.createMerchantBusiness(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantBusiness.Builder builder = ApiResponseMerchantBusiness.newBuilder()
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
    public Uni<ApiResponseMerchantBusiness> update(
            pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest request) {
        if (request.getMerchantBusinessInfoId() <= 0) {
            return IdValidator.invalid("MerchantBusinessInfo id");
        }        UpdateMerchantBusinessRequest domainReq = new UpdateMerchantBusinessRequest();
        domainReq.setMerchantBusinessInfoId(request.getMerchantBusinessInfoId());
        domainReq.setBusinessType(request.getBusinessType());
        domainReq.setTaxId(request.getTaxId());
        domainReq.setEstablishedYear(request.getEstablishedYear());
        domainReq.setNumberOfEmployees(request.getNumberOfEmployees());
        domainReq.setWebsiteUrl(request.getWebsiteUrl());

        return merchantBusinessCommandService.updateMerchantBusiness(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantBusiness.Builder builder = ApiResponseMerchantBusiness.newBuilder()
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
    public Uni<ApiResponseMerchantBusinessDeleteAt> trashedMerchantBusiness(FindByIdMerchantBusinessRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantBusinessCommandService.trashedMerchantBusiness((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantBusinessDeleteAt.Builder builder = ApiResponseMerchantBusinessDeleteAt
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
    public Uni<ApiResponseMerchantBusinessDeleteAt> restoreMerchantBusiness(FindByIdMerchantBusinessRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantBusinessCommandService.restoreMerchantBusiness((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantBusinessDeleteAt.Builder builder = ApiResponseMerchantBusinessDeleteAt
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantDelete> deleteMerchantBusinessPermanent(
            FindByIdMerchantBusinessRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantBusinessCommandService.deleteMerchantBusinessPermanent((long) request.getId())
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> restoreAllMerchantBusiness(
            com.google.protobuf.Empty request) {
        return merchantBusinessCommandService.restoreAllMerchantBusiness()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> deleteAllMerchantBusinessPermanent(
            com.google.protobuf.Empty request) {
        return merchantBusinessCommandService.deleteAllMerchantBusinessPermanent()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse toProto(MerchantBusinessResponse r) {
        if (r == null) {
            return pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse.getDefaultInstance();
        }
        pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse.Builder builder = pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getBusinessType() != null) {
            builder.setBusinessType(r.getBusinessType());
        }
        if (r.getTaxId() != null) {
            builder.setTaxId(r.getTaxId());
        }
        if (r.getEstablishedYear() != null) {
            builder.setEstablishedYear(r.getEstablishedYear());
        }
        if (r.getNumberOfEmployees() != null) {
            builder.setNumberOfEmployees(r.getNumberOfEmployees());
        }
        if (r.getWebsiteUrl() != null) {
            builder.setWebsiteUrl(r.getWebsiteUrl());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt toProto(
            MerchantBusinessResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt.Builder builder = pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getBusinessType() != null) {
            builder.setBusinessType(r.getBusinessType());
        }
        if (r.getTaxId() != null) {
            builder.setTaxId(r.getTaxId());
        }
        if (r.getEstablishedYear() != null) {
            builder.setEstablishedYear(r.getEstablishedYear());
        }
        if (r.getNumberOfEmployees() != null) {
            builder.setNumberOfEmployees(r.getNumberOfEmployees());
        }
        if (r.getWebsiteUrl() != null) {
            builder.setWebsiteUrl(r.getWebsiteUrl());
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
