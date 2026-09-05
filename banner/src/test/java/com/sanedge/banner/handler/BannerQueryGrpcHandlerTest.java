package com.sanedge.banner.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.entity.Banner;
import com.sanedge.banner.service.BannerQueryService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BannerQueryGrpcHandlerTest {

        @Mock
        private BannerQueryService bannerQueryService;

        private BannerQueryGrpcHandler bannerQueryGrpcHandler;

        @BeforeEach
        void setUp() throws Exception {
                bannerQueryGrpcHandler = new BannerQueryGrpcHandler();
                Field serviceField = BannerQueryGrpcHandler.class.getDeclaredField("bannerQueryService");
                serviceField.setAccessible(true);
                serviceField.set(bannerQueryGrpcHandler, bannerQueryService);
        }

        @Test
        void findAll_Success() {
                pb.banner.BannerQuery.FindAllBannerRequest grpcRequest = pb.banner.BannerQuery.FindAllBannerRequest
                                .newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("")
                                .build();

                Banner banner = createTestBanner(1L, "Test Banner");
                BannerResponse bannerResponse = BannerResponse.from(banner);
                PaginationMeta paginationMeta = new PaginationMeta(1, 10, 1, 1);
                ApiResponsePagination<List<BannerResponse>> serviceResponse = new ApiResponsePagination<>(
                                "success",
                                "Banners retrieved successfully",
                                List.of(bannerResponse),
                                paginationMeta);

                lenient().when(bannerQueryService.findAllPaginated(any(FindAllBannerRequest.class)))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponsePaginationBanner response = bannerQueryGrpcHandler
                                .findAll(grpcRequest).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banners retrieved successfully");
                assertThat(response.getDataList()).hasSize(1);
                assertThat(response.getData(0).getName()).isEqualTo("Test Banner");
        }

        @Test
        void findById_Success() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                Banner banner = createTestBanner(1L, "Test Banner");
                ApiResponse<BannerResponse> serviceResponse = ApiResponse.success("Banner found",
                                BannerResponse.from(banner));

                lenient().when(bannerQueryService.findById(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBanner response = bannerQueryGrpcHandler.findById(grpcRequest).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner found");
                assertThat(response.getData().getName()).isEqualTo("Test Banner");
        }

        @Test
        void findById_NotFound_ReturnsStatusNotFound() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                lenient().when(bannerQueryService.findById(999L))
                                .thenReturn(Uni.createFrom()
                                                .failure(new NotFoundException("Banner not found with id: 999")));

                assertThatThrownBy(() -> bannerQueryGrpcHandler.findById(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                                        assertThat(sre.getStatus().getDescription())
                                                        .contains("Banner not found with id: 999");
                                });
        }

        @Test
        void findByActive_Success() {
                pb.banner.BannerQuery.FindAllBannerRequest grpcRequest = pb.banner.BannerQuery.FindAllBannerRequest
                                .newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("")
                                .build();

                Banner banner = createTestBanner(1L, "Active Banner");
                banner.setIsActive(true);
                BannerResponseDeleteAt bannerResponse = BannerResponseDeleteAt.from(banner);
                PaginationMeta paginationMeta = new PaginationMeta(1, 10, 1, 1);
                ApiResponsePagination<List<BannerResponseDeleteAt>> serviceResponse = new ApiResponsePagination<>(
                                "success",
                                "Active banners retrieved successfully",
                                List.of(bannerResponse),
                                paginationMeta);

                lenient().when(bannerQueryService.findActivePaginated(any(FindAllBannerRequest.class)))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponsePaginationBannerDeleteAt response = bannerQueryGrpcHandler
                                .findByActive(grpcRequest).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Active banners retrieved successfully");
                assertThat(response.getDataList()).hasSize(1);
                assertThat(response.getData(0).getName()).isEqualTo("Active Banner");
        }

        @Test
        void findByTrashed_Success() {
                pb.banner.BannerQuery.FindAllBannerRequest grpcRequest = pb.banner.BannerQuery.FindAllBannerRequest
                                .newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("")
                                .build();

                Banner banner = createTestBanner(1L, "Trashed Banner");
                banner.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                BannerResponseDeleteAt bannerResponse = BannerResponseDeleteAt.from(banner);
                PaginationMeta paginationMeta = new PaginationMeta(1, 10, 1, 1);
                ApiResponsePagination<List<BannerResponseDeleteAt>> serviceResponse = new ApiResponsePagination<>(
                                "success",
                                "Trashed banners retrieved successfully",
                                List.of(bannerResponse),
                                paginationMeta);

                lenient().when(bannerQueryService.findTrashedPaginated(any(FindAllBannerRequest.class)))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponsePaginationBannerDeleteAt response = bannerQueryGrpcHandler
                                .findByTrashed(grpcRequest).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Trashed banners retrieved successfully");
                assertThat(response.getDataList()).hasSize(1);
                assertThat(response.getData(0).getName()).isEqualTo("Trashed Banner");
        }

        private Banner createTestBanner(Long id, String name) {
                Banner banner = new Banner();
                try {
                        Field idField = banner.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(banner, id);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to set banner id", e);
                }
                banner.setName(name);
                banner.setStartDate(Date.valueOf(LocalDate.now()));
                banner.setEndDate(Date.valueOf(LocalDate.now().plusMonths(1)));
                banner.setStartTime(Time.valueOf(LocalTime.of(9, 0)));
                banner.setEndTime(Time.valueOf(LocalTime.of(18, 0)));
                banner.setIsActive(true);
                banner.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                banner.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return banner;
        }
}
