package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantDocumentServiceImpl implements MerchantDocumentService {

    private static final Logger LOG = Logger.getLogger(MerchantDocumentServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;

    @GrpcClient("merchant")
    pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.document.findAll", () -> merchantDocumentQueryService.findAll(
                pb.merchant_document.MerchantDocumentQuery.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDocumentDto.ApiResponsePaginationDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAllActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.document.findAllActive", () -> merchantDocumentQueryService.findAllActive(
                pb.merchant_document.MerchantDocumentQuery.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDocumentDto.ApiResponsePaginationDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAllTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.document.findAllTrashed", () -> merchantDocumentQueryService.findAllTrashed(
                pb.merchant_document.MerchantDocumentQuery.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDocumentDto.ApiResponsePaginationDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> findById(int id) {
        return telemetryHelper.traceAndMetric("merchant.document.findById", () -> merchantDocumentQueryService.findById(
                pb.merchant_document.MerchantDocumentQuery.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find merchant document by id " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> create(MerchantDocumentDto.CreateRequest body) {
        return telemetryHelper.traceAndMetric("merchant.document.create", () -> merchantDocumentCommandService.create(
                pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
                        .setMerchantId(body.merchantId())
                        .setDocumentType(body.documentType() == null ? "" : body.documentType())
                        .setDocumentUrl(body.documentUrl() == null ? "" : body.documentUrl())
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> update(int id, MerchantDocumentDto.UpdateRequest body) {
        return telemetryHelper.traceAndMetric("merchant.document.update", () -> merchantDocumentCommandService.update(
                pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
                        .setDocumentId(id)
                        .setMerchantId(body.merchantId())
                        .setDocumentType(body.documentType() == null ? "" : body.documentType())
                        .setDocumentUrl(body.documentUrl() == null ? "" : body.documentUrl())
                        .setNote(body.note() == null ? "" : body.note())
                        .setStatus(body.status() == null ? "" : body.status())
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant document " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> updateStatus(int id, MerchantDocumentDto.UpdateStatusRequest body) {
        return telemetryHelper.traceAndMetric("merchant.document.updateStatus", () -> merchantDocumentCommandService.updateStatus(
                pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest.newBuilder()
                        .setDocumentId(id)
                        .setMerchantId(body.merchantId())
                        .setNote(body.note() == null ? "" : body.note())
                        .setStatus(body.status() == null ? "" : body.status())
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant document status " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> trash(int id) {
        return telemetryHelper.traceAndMetric("merchant.document.trash", () -> merchantDocumentCommandService.trashed(
                pb.merchant_document.MerchantDocumentCommand.TrashedMerchantDocumentRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash merchant document " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseDocument> restore(int id) {
        return telemetryHelper.traceAndMetric("merchant.document.restore", () -> merchantDocumentCommandService.restore(
                pb.merchant_document.MerchantDocumentCommand.RestoreMerchantDocumentRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(MerchantDocumentDto.ApiResponseDocument::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore merchant document " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.document.deletePermanent", () -> merchantDocumentCommandService.deletePermanent(
                pb.merchant_document.MerchantDocumentCommand.DeleteMerchantDocumentPermanentRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(MerchantDocumentDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete merchant document permanently " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("merchant.document.restoreAll", () -> merchantDocumentCommandService.restoreAll(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDocumentDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("merchant.document.deleteAllPermanent", () -> merchantDocumentCommandService.deleteAllPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDocumentDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all merchant documents: " + throwable.getMessage(), throwable)));
    }
}
