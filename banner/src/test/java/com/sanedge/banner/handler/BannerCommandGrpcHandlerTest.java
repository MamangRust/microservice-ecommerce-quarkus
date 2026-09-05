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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.banner.domain.requests.CreateBannerRequest;
import com.sanedge.banner.domain.requests.UpdateBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.entity.Banner;
import com.sanedge.banner.service.BannerCommandService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class BannerCommandGrpcHandlerTest {

        @Mock
        private BannerCommandService bannerCommandService;

        private BannerCommandGrpcHandler bannerCommandGrpcHandler;

        @BeforeEach
        void setUp() throws Exception {
                bannerCommandGrpcHandler = new BannerCommandGrpcHandler();
                Field serviceField = BannerCommandGrpcHandler.class.getDeclaredField("bannerCommandService");
                serviceField.setAccessible(true);
                serviceField.set(bannerCommandGrpcHandler, bannerCommandService);
        }

        @Test
        void create_Success() {
                pb.banner.BannerCommand.CreateBannerRequest grpcRequest = pb.banner.BannerCommand.CreateBannerRequest
                                .newBuilder()
                                .setName("Test Banner")
                                .setStartDate("2026-06-01")
                                .setEndDate("2026-08-31")
                                .setStartTime("09:00")
                                .setEndTime("18:00")
                                .setIsActive(true)
                                .build();

                Banner banner = createTestBanner(1L, "Test Banner");
                ApiResponse<BannerResponse> serviceResponse = ApiResponse.success("Banner created successfully!",
                                BannerResponse.from(banner));

                lenient().when(bannerCommandService.createBanner(any(CreateBannerRequest.class)))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBanner response = bannerCommandGrpcHandler.create(grpcRequest).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner created successfully!");
                assertThat(response.getData().getName()).isEqualTo("Test Banner");
        }

        @Test
        void create_AlreadyExists_ReturnsStatusAlreadyExists() {
                pb.banner.BannerCommand.CreateBannerRequest grpcRequest = pb.banner.BannerCommand.CreateBannerRequest
                                .newBuilder()
                                .setName("Existing Banner")
                                .setStartDate("2026-06-01")
                                .setEndDate("2026-08-31")
                                .setStartTime("09:00")
                                .setEndTime("18:00")
                                .setIsActive(true)
                                .build();

                lenient().when(bannerCommandService.createBanner(any(CreateBannerRequest.class)))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceAlreadyExistsException("Banner already exists")));

                assertThatThrownBy(() -> bannerCommandGrpcHandler.create(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode())
                                                        .isEqualTo(Status.ALREADY_EXISTS.getCode());
                                        assertThat(sre.getStatus().getDescription()).contains("Banner already exists");
                                });
        }

        @Test
        void update_Success() {
                pb.banner.BannerCommand.UpdateBannerRequest grpcRequest = pb.banner.BannerCommand.UpdateBannerRequest
                                .newBuilder()
                                .setBannerId(1)
                                .setName("Updated Banner")
                                .setStartDate("2026-07-01")
                                .setEndDate("2026-09-30")
                                .setStartTime("10:00")
                                .setEndTime("20:00")
                                .setIsActive(false)
                                .build();

                Banner banner = createTestBanner(1L, "Updated Banner");
                ApiResponse<BannerResponse> serviceResponse = ApiResponse.success("Banner updated successfully!",
                                BannerResponse.from(banner));

                lenient().when(bannerCommandService.updateBanner(any(UpdateBannerRequest.class)))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBanner response = bannerCommandGrpcHandler.update(grpcRequest).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner updated successfully!");
        }

        @Test
        void update_NotFound_ReturnsStatusNotFound() {
                pb.banner.BannerCommand.UpdateBannerRequest grpcRequest = pb.banner.BannerCommand.UpdateBannerRequest
                                .newBuilder()
                                .setBannerId(999)
                                .setName("Non-existent Banner")
                                .setStartDate("2026-07-01")
                                .setEndDate("2026-09-30")
                                .setStartTime("10:00")
                                .setEndTime("20:00")
                                .setIsActive(false)
                                .build();

                lenient().when(bannerCommandService.updateBanner(any(UpdateBannerRequest.class)))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Banner not found")));

                assertThatThrownBy(() -> bannerCommandGrpcHandler.update(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                                        assertThat(sre.getStatus().getDescription()).contains("Banner not found");
                                });
        }

        @Test
        void trash_Success() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                Banner trashedBanner = createTestBanner(1L, "Trashed Banner");
                trashedBanner.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                ApiResponse<BannerResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Banner trashed successfully!", BannerResponseDeleteAt.from(trashedBanner));

                lenient().when(bannerCommandService.trashedBanner(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBannerDeleteAt response = bannerCommandGrpcHandler.trash(grpcRequest)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner trashed successfully!");
                assertThat(response.getData().getDeletedAt().getValue()).isNotEmpty();
        }

        @Test
        void trash_NotFound_ReturnsStatusNotFound() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                lenient().when(bannerCommandService.trashedBanner(999L))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Trashed banner not found")));

                assertThatThrownBy(() -> bannerCommandGrpcHandler.trash(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                                        assertThat(sre.getStatus().getDescription())
                                                        .contains("Trashed banner not found");
                                });
        }

        @Test
        void restore_Success() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                Banner restoredBanner = createTestBanner(1L, "Restored Banner");

                ApiResponse<BannerResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Banner restored successfully!", BannerResponseDeleteAt.from(restoredBanner));

                lenient().when(bannerCommandService.restoreBanner(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBannerDeleteAt response = bannerCommandGrpcHandler
                                .restore(grpcRequest).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner restored successfully!");
        }

        @Test
        void restore_NotFound_ReturnsStatusNotFound() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                lenient().when(bannerCommandService.restoreBanner(999L))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Restore banner not found")));

                assertThatThrownBy(() -> bannerCommandGrpcHandler.restore(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                                        assertThat(sre.getStatus().getDescription())
                                                        .contains("Restore banner not found");
                                });
        }

        @Test
        void deletePermanent_Success() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                ApiResponse<Void> serviceResponse = ApiResponse.success("Banner deleted permanently!");

                lenient().when(bannerCommandService.deleteBannerPermanent(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBannerDelete response = bannerCommandGrpcHandler
                                .deletePermanent(grpcRequest).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Banner deleted permanently!");
        }

        @Test
        void deletePermanent_NotFound_ReturnsStatusNotFound() {
                pb.banner.BannerCommon.FindByIdBannerRequest grpcRequest = pb.banner.BannerCommon.FindByIdBannerRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                lenient().when(bannerCommandService.deleteBannerPermanent(999L))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Banner not found")));

                assertThatThrownBy(() -> bannerCommandGrpcHandler.deletePermanent(grpcRequest).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                                        assertThat(sre.getStatus().getDescription()).contains("Banner not found");
                                });
        }

        @Test
        void restoreAll_Success() {
                com.google.protobuf.Empty grpcRequest = com.google.protobuf.Empty.getDefaultInstance();

                ApiResponse<Void> serviceResponse = ApiResponse.success("All banners restored successfully!");

                lenient().when(bannerCommandService.restoreAllBanner())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBannerAll response = bannerCommandGrpcHandler.restoreAll(grpcRequest)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All banners restored successfully!");
        }

        @Test
        void deleteAll_Success() {
                com.google.protobuf.Empty grpcRequest = com.google.protobuf.Empty.getDefaultInstance();

                ApiResponse<Void> serviceResponse = ApiResponse.success("All banners permanently deleted!");

                lenient().when(bannerCommandService.deleteAllBannerPermanent())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.banner.BannerCommon.ApiResponseBannerAll response = bannerCommandGrpcHandler.deleteAll(grpcRequest)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All banners permanently deleted!");
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
