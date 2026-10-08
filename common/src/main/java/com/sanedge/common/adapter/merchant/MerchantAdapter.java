package com.sanedge.common.adapter.merchant;

import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.merchant.MerchantCommon.ApiResponseMerchant;
import pb.merchant.MerchantCommon.FindByIdMerchantRequest;
import pb.merchant.MerchantCommon.MerchantResponse;
import pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub;

@ApplicationScoped
public class MerchantAdapter implements MerchantPort {

    @GrpcClient("merchant")
    MutinyMerchantQueryServiceStub client;

    @Override
    public Uni<Merchant> findById(int merchantId) {
        return client.findById(FindByIdMerchantRequest.newBuilder().setId(merchantId).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("Merchant not found: " + merchantId);
                    }
                    return toModel(resp.getData());
                });
    }

    static Merchant toModel(MerchantResponse m) {
        if (m == null) {
            return null;
        }
        return new Merchant(
                m.getId(),
                m.getUserId(),
                m.getName(),
                m.getDescription(),
                m.getAddress(),
                m.getContactEmail(),
                m.getContactPhone(),
                m.getStatus(),
                ProtoTime.parse(m.getCreatedAt()),
                ProtoTime.parse(m.getUpdatedAt()));
    }

    static Merchant toModel(ApiResponseMerchant resp) {
        return resp != null && resp.hasData() ? toModel(resp.getData()) : null;
    }
}
