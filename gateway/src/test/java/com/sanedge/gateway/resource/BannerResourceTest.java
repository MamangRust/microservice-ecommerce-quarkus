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

import com.sanedge.gateway.dto.BannerDto;
import com.sanedge.gateway.service.BannerService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class BannerResourceTest {
    @Mock
    BannerService bannerService;
    private BannerResource bannerResource;

    @BeforeEach
    void setUp() throws Exception {
        bannerResource = new BannerResource();
        Field f = BannerResource.class.getDeclaredField("bannerService");
        f.setAccessible(true);
        f.set(bannerResource, bannerService);
    }

    private BannerDto.BannerResponse mk(int id) {
        return new BannerDto.BannerResponse(id, "name", "", "", "", "", true, "", "");
    }

    @Test
    void findAll_Success() {
        BannerDto.ApiResponsePaginationBanner dto = new BannerDto.ApiResponsePaginationBanner(
                List.of(mk(1)), "success", "ok");
        lenient().when(bannerService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        BannerDto.ApiResponsePaginationBanner dto = new BannerDto.ApiResponsePaginationBanner(
                List.of(), "success", "ok");
        lenient().when(bannerService.findByActive(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        BannerDto.ApiResponsePaginationBanner dto = new BannerDto.ApiResponsePaginationBanner(
                List.of(), "success", "ok");
        lenient().when(bannerService.findByTrashed(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        BannerDto.ApiResponseBanner dto = new BannerDto.ApiResponseBanner(mk(1), "success", "ok");
        lenient().when(bannerService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createBanner_Success_Returns201() {
        BannerDto.ApiResponseBanner dto = new BannerDto.ApiResponseBanner(mk(1), "success", "ok");
        lenient().when(bannerService.createBanner(any())).thenReturn(Uni.createFrom().item(dto));
        pb.banner.BannerCommand.CreateBannerRequest req = pb.banner.BannerCommand.CreateBannerRequest.newBuilder().build();
        Response r = bannerResource.createBanner(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateBanner_Success() {
        BannerDto.ApiResponseBanner dto = new BannerDto.ApiResponseBanner(mk(1), "success", "ok");
        lenient().when(bannerService.updateBanner(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.banner.BannerCommand.UpdateBannerRequest req = pb.banner.BannerCommand.UpdateBannerRequest.newBuilder().build();
        Response r = bannerResource.updateBanner(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteBanner_Success() {
        BannerDto.ApiResponseBanner dto = new BannerDto.ApiResponseBanner(mk(1), "success", "ok");
        lenient().when(bannerService.deleteBanner(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.deleteBanner(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreBanner_Success() {
        BannerDto.ApiResponseBanner dto = new BannerDto.ApiResponseBanner(mk(1), "success", "ok");
        lenient().when(bannerService.restoreBanner(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.restoreBanner(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteBannerPermanent_Success() {
        BannerDto.SimpleResponse dto = new BannerDto.SimpleResponse("success", "ok");
        lenient().when(bannerService.deleteBannerPermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.deleteBannerPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllBanners_Success() {
        BannerDto.SimpleResponse dto = new BannerDto.SimpleResponse("success", "ok");
        lenient().when(bannerService.restoreAllBanners()).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.restoreAllBanners().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllBannersPermanent_Success() {
        BannerDto.SimpleResponse dto = new BannerDto.SimpleResponse("success", "ok");
        lenient().when(bannerService.deleteAllBannersPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = bannerResource.deleteAllBannersPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
