package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantPolicyDto;
import io.smallrye.mutiny.Uni;

public interface MerchantPolicyService {
    Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findAll(int page, int size, String search);
    Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findByActive(int page, int size, String search);
    Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findByTrashed(int page, int size, String search);
    Uni<MerchantPolicyDto.ApiResponsePolicy> findById(int id);
    Uni<MerchantPolicyDto.ApiResponsePolicy> create(pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest body);
    Uni<MerchantPolicyDto.ApiResponsePolicy> update(int id, pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest body);
    Uni<MerchantPolicyDto.ApiResponsePolicy> delete(int id);
    Uni<MerchantPolicyDto.ApiResponsePolicy> restore(int id);
    Uni<MerchantPolicyDto.SimpleResponse> deletePermanent(int id);
    Uni<MerchantPolicyDto.SimpleResponse> restoreAll();
    Uni<MerchantPolicyDto.SimpleResponse> deleteAllPermanent();
}
