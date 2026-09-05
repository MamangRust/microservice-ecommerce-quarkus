package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import io.smallrye.mutiny.Uni;

public interface MerchantDocumentService {
    Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAll(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAllActive(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponsePaginationDocument> findAllTrashed(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponseDocument> findById(int id);
    Uni<MerchantDocumentDto.ApiResponseDocument> create(MerchantDocumentDto.CreateRequest body);
    Uni<MerchantDocumentDto.ApiResponseDocument> update(int id, MerchantDocumentDto.UpdateRequest body);
    Uni<MerchantDocumentDto.ApiResponseDocument> updateStatus(int id, MerchantDocumentDto.UpdateStatusRequest body);
    Uni<MerchantDocumentDto.ApiResponseDocument> trash(int id);
    Uni<MerchantDocumentDto.ApiResponseDocument> restore(int id);
    Uni<MerchantDocumentDto.SimpleResponse> deletePermanent(int id);
    Uni<MerchantDocumentDto.SimpleResponse> restoreAll();
    Uni<MerchantDocumentDto.SimpleResponse> deleteAllPermanent();
}
