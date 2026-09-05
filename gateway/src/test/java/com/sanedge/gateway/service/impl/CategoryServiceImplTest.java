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

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.category.MutinyCategoryQueryServiceGrpc.MutinyCategoryQueryServiceStub categoryQueryService;
    @Mock
    private pb.category.MutinyCategoryCommandServiceGrpc.MutinyCategoryCommandServiceStub categoryCommandService;

    private CategoryServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = CategoryServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private void injectNull(String name) throws Exception {
        inject(name, null);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new CategoryServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("categoryQueryService", categoryQueryService);
        inject("categoryCommandService", categoryCommandService);

        injectNull("categoryPriceService");
        injectNull("categoryPriceByIdService");
        injectNull("categoryPriceByMerchantService");
        injectNull("categoryTotalPriceService");
        injectNull("categoryTotalPriceByIdService");
        injectNull("categoryTotalPriceByMerchantService");
    }

    @Test
    void findById_PropagatesCategoryResponse() {
        pb.category.CategoryCommon.ApiResponseCategory proto = pb.category.CategoryCommon.ApiResponseCategory.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(categoryQueryService.findById(any(pb.category.CategoryCommon.FindByIdCategoryRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesCategoryResponse() {
        pb.category.CategoryCommon.ApiResponseCategory proto = pb.category.CategoryCommon.ApiResponseCategory.newBuilder()
                .setStatus("success").setMessage("created").build();
        CategoryDto.CreateCategoryRequest req = new CategoryDto.CreateCategoryRequest("name", "desc", "slug", "image");
        lenient().when(categoryCommandService.create(any(pb.category.CategoryCommand.CreateCategoryRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void trash_TrashStub_Propagates() {
        pb.category.CategoryCommon.ApiResponseCategoryDeleteAt proto = pb.category.CategoryCommon.ApiResponseCategoryDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(categoryCommandService.trashedCategory(any(pb.category.CategoryCommon.FindByIdCategoryRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.trash(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.category.CategoryCommon.ApiResponseCategoryDeleteAt proto = pb.category.CategoryCommon.ApiResponseCategoryDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(categoryCommandService.restoreCategory(any(pb.category.CategoryCommon.FindByIdCategoryRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
