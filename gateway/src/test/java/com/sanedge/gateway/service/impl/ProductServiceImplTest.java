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
class ProductServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.product.MutinyProductQueryServiceGrpc.MutinyProductQueryServiceStub productQueryService;
    @Mock
    private pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    private ProductServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = ProductServiceImpl.class.getDeclaredField(name);
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
        service = new ProductServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("productQueryService", productQueryService);
        inject("productCommandService", productCommandService);
    }

    @Test
    void findById_PropagatesProductResponse() {
        pb.product.ProductCommon.ApiResponseProduct proto = pb.product.ProductCommon.ApiResponseProduct.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(productQueryService.findById(any(pb.product.ProductCommon.FindByIdProductRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesProductResponse() {
        pb.product.ProductCommon.ApiResponseProduct proto = pb.product.ProductCommon.ApiResponseProduct.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.product.ProductCommand.CreateProductRequest req = pb.product.ProductCommand.CreateProductRequest.newBuilder().build();
        lenient().when(productCommandService.create(any(pb.product.ProductCommand.CreateProductRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.product.ProductCommon.ApiResponseProductDeleteAt proto = pb.product.ProductCommon.ApiResponseProductDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(productCommandService.trashedProduct(any(pb.product.ProductCommon.FindByIdProductRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void deletePermanent_PropagatesSimpleResponse() {
        pb.product.ProductCommon.ApiResponseProductDelete proto = pb.product.ProductCommon.ApiResponseProductDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(productCommandService.deleteProductPermanent(any(pb.product.ProductCommon.FindByIdProductRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deletePermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.product.ProductCommon.ApiResponseProductDeleteAt proto = pb.product.ProductCommon.ApiResponseProductDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(productCommandService.restoreProduct(any(pb.product.ProductCommon.FindByIdProductRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
