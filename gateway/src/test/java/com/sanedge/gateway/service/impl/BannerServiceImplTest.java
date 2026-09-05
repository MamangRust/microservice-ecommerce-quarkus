package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.banner.MutinyBannerQueryServiceGrpc.MutinyBannerQueryServiceStub bannerQueryService;
    @Mock
    private pb.banner.MutinyBannerCommandServiceGrpc.MutinyBannerCommandServiceStub bannerCommandService;

    private BannerServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = BannerServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new BannerServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("bannerQueryService", bannerQueryService);
        inject("bannerCommandService", bannerCommandService);
    }

    @Test
    void findById_PropagatesBannerResponse() {
        pb.banner.BannerCommon.ApiResponseBanner proto = pb.banner.BannerCommon.ApiResponseBanner.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(bannerQueryService.findById(any(pb.banner.BannerCommon.FindByIdBannerRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("ok");
    }

    @Test
    void create_PropagatesBannerResponse() {
        pb.banner.BannerCommon.ApiResponseBanner proto = pb.banner.BannerCommon.ApiResponseBanner.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.banner.BannerCommand.CreateBannerRequest req = pb.banner.BannerCommand.CreateBannerRequest.newBuilder().build();
        lenient().when(bannerCommandService.create(any(pb.banner.BannerCommand.CreateBannerRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.createBanner(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteBanner_TrashStub_Propagates() {
        pb.banner.BannerCommon.ApiResponseBannerDeleteAt proto = pb.banner.BannerCommon.ApiResponseBannerDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(bannerCommandService.trash(any(pb.banner.BannerCommon.FindByIdBannerRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteBanner(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void deletePermanent_PropagatesSimpleResponse() {
        pb.banner.BannerCommon.ApiResponseBannerDelete proto = pb.banner.BannerCommon.ApiResponseBannerDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(bannerCommandService.deletePermanent(any(pb.banner.BannerCommon.FindByIdBannerRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteBannerPermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.banner.BannerCommon.ApiResponseBannerDeleteAt proto = pb.banner.BannerCommon.ApiResponseBannerDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(bannerCommandService.restore(any(pb.banner.BannerCommon.FindByIdBannerRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restoreBanner(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
