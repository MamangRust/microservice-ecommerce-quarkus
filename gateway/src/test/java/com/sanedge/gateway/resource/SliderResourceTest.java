package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.SliderDto;
import com.sanedge.gateway.service.SliderService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class SliderResourceTest {
    @Mock
    SliderService sliderService;
    private SliderResource sliderResource;

    @BeforeEach
    void setUp() throws Exception {
        sliderResource = new SliderResource();
        Field f = SliderResource.class.getDeclaredField("sliderService");
        f.setAccessible(true);
        f.set(sliderResource, sliderService);
    }

    private SliderDto.SliderResponse mk(int id) {
        return new SliderDto.SliderResponse(id, "name", "", "", "");
    }

    @Test
    void findAll_Success() {
        SliderDto.ApiResponsePaginationSlider dto = new SliderDto.ApiResponsePaginationSlider(
                List.of(mk(1)), "success", "ok");
        lenient().when(sliderService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        SliderDto.ApiResponsePaginationSlider dto = new SliderDto.ApiResponsePaginationSlider(
                List.of(), "success", "ok");
        lenient().when(sliderService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        SliderDto.ApiResponsePaginationSlider dto = new SliderDto.ApiResponsePaginationSlider(
                List.of(), "success", "ok");
        lenient().when(sliderService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        SliderDto.ApiResponseSlider dto = new SliderDto.ApiResponseSlider(mk(1), "success", "ok");
        lenient().when(sliderService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createSlider_Success_Returns201() {
        SliderDto.ApiResponseSlider dto = new SliderDto.ApiResponseSlider(mk(1), "success", "ok");
        lenient().when(sliderService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.slider.SliderCommand.CreateSliderRequest req = pb.slider.SliderCommand.CreateSliderRequest.newBuilder().build();
        Response r = sliderResource.createSlider(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateSlider_Success() {
        SliderDto.ApiResponseSlider dto = new SliderDto.ApiResponseSlider(mk(1), "success", "ok");
        lenient().when(sliderService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.slider.SliderCommand.UpdateSliderRequest req = pb.slider.SliderCommand.UpdateSliderRequest.newBuilder().build();
        Response r = sliderResource.updateSlider(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteSlider_Success() {
        SliderDto.ApiResponseSlider dto = new SliderDto.ApiResponseSlider(mk(1), "success", "ok");
        lenient().when(sliderService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.deleteSlider(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreSlider_Success() {
        SliderDto.ApiResponseSlider dto = new SliderDto.ApiResponseSlider(mk(1), "success", "ok");
        lenient().when(sliderService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.restoreSlider(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteSliderPermanent_Success() {
        SliderDto.SimpleResponse dto = new SliderDto.SimpleResponse("success", "ok");
        lenient().when(sliderService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.deleteSliderPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllSliders_Success() {
        SliderDto.SimpleResponse dto = new SliderDto.SimpleResponse("success", "ok");
        lenient().when(sliderService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.restoreAllSliders().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllSlidersPermanent_Success() {
        SliderDto.SimpleResponse dto = new SliderDto.SimpleResponse("success", "ok");
        lenient().when(sliderService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = sliderResource.deleteAllSlidersPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
