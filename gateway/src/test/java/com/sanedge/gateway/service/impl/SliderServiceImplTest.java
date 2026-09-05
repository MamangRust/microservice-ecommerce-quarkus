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
class SliderServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.slider.MutinySliderQueryServiceGrpc.MutinySliderQueryServiceStub sliderQueryService;
    @Mock
    private pb.slider.MutinySliderCommandServiceGrpc.MutinySliderCommandServiceStub sliderCommandService;

    private SliderServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = SliderServiceImpl.class.getDeclaredField(name);
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
        service = new SliderServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("sliderQueryService", sliderQueryService);
        inject("sliderCommandService", sliderCommandService);
    }

    @Test
    void findById_PropagatesSliderResponse() {
        pb.slider.SliderCommon.ApiResponseSlider proto = pb.slider.SliderCommon.ApiResponseSlider.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(sliderQueryService.findById(any(pb.slider.SliderCommon.FindByIdSliderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesSliderResponse() {
        pb.slider.SliderCommon.ApiResponseSlider proto = pb.slider.SliderCommon.ApiResponseSlider.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.slider.SliderCommand.CreateSliderRequest req = pb.slider.SliderCommand.CreateSliderRequest.newBuilder().build();
        lenient().when(sliderCommandService.create(any(pb.slider.SliderCommand.CreateSliderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.slider.SliderCommon.ApiResponseSliderDeleteAt proto = pb.slider.SliderCommon.ApiResponseSliderDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(sliderCommandService.trashedSlider(any(pb.slider.SliderCommon.FindByIdSliderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.slider.SliderCommon.ApiResponseSliderDeleteAt proto = pb.slider.SliderCommon.ApiResponseSliderDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(sliderCommandService.restoreSlider(any(pb.slider.SliderCommon.FindByIdSliderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
