package com.sanedge.shipping_address.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;
import com.sanedge.shipping_address.entity.ShippingAddress;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.shipping_address.repository.ShippingAddressCommandRepository;
import com.sanedge.shipping_address.service.ShippingAddressCommand;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShippingAddressCommandImplService implements ShippingAddressCommand {
        private static final Logger logger = LoggerFactory.getLogger(ShippingAddressCommandImplService.class);

        ShippingAddressCommandRepository shippingAddressCommandRepository;
        RedisService redisService;
        TracingMetrics tracingMetrics;

        @Inject
        public ShippingAddressCommandImplService(ShippingAddressCommandRepository shippingAddressCommandRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.shippingAddressCommandRepository = shippingAddressCommandRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        private Uni<Void> invalidateCache(Long shippingId) {
                if (shippingId != null) {
                        return redisService.deleteReactive("shipping:id:" + shippingId).replaceWithVoid();
                }
                return Uni.createFrom().voidItem();
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<ShippingAddressResponseDeleteAt>> trash(Integer shippingId) {
                logger.info("Trashing shipping address id={}", shippingId);

                return tracingMetrics.traceAndMeasure("trashShippingAddress", "trash_shipping_address",
                                Attributes.builder().put("shipping.id", shippingId.toString()).build(),
                                () -> shippingAddressCommandRepository.trash(shippingId.longValue())
                                                .chain(address -> {
                                                        if (address == null) {
                                                                throw new ResourceNotFoundException(
                                                                                "Shipping address not found or already trashed");
                                                        }
                                                        ShippingAddressResponseDeleteAt response = ShippingAddressResponseDeleteAt
                                                                        .from(address);

                                                        return invalidateCache(shippingId.longValue())
                                                                        .map(v -> {
                                                                                logger.info("Successfully trashed shipping address with ID: {}",
                                                                                                shippingId);
                                                                                return ApiResponse.success(
                                                                                                "Shipping address trashed successfully",
                                                                                                response);
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<ShippingAddressResponseDeleteAt>> restore(Integer shippingId) {
                logger.info("Restoring shipping address id={}", shippingId);

                return tracingMetrics.traceAndMeasure("restoreShippingAddress", "restore_shipping_address",
                                Attributes.builder().put("shipping.id", shippingId.toString()).build(),
                                () -> shippingAddressCommandRepository.restore(shippingId.longValue())
                                                .chain(address -> {
                                                        if (address == null) {
                                                                throw new ResourceNotFoundException(
                                                                                "Shipping address not found or not trashed");
                                                        }
                                                        ShippingAddressResponseDeleteAt response = ShippingAddressResponseDeleteAt
                                                                        .from(address);

                                                        return invalidateCache(shippingId.longValue())
                                                                        .map(v -> {
                                                                                logger.info("Successfully restored shipping address with ID: {}",
                                                                                                shippingId);
                                                                                return ApiResponse.success(
                                                                                                "Shipping address restored successfully",
                                                                                                response);
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deletePermanently(Integer shippingId) {
                Attributes attrs = Attributes.builder().put("shipping.id", shippingId.toString()).build();
                logger.warn("Permanently deleting shipping address id={}", shippingId);

                return tracingMetrics.traceAndMeasure("deleteShippingAddressPermanent",
                                "delete_shipping_address_permanent", attrs,
                                () -> shippingAddressCommandRepository.deletePermanent(shippingId.longValue())
                                                .chain(deletedAddress -> {
                                                        if (deletedAddress == null) {
                                                                logger.warn("Permanent delete failed - shipping address not found or must be trashed before permanent deletion with id: {}", shippingId);
                                                                throw new InvalidRequestException("Shipping address not found or must be trashed before permanent deletion");
                                                        }
                                                        return invalidateCache(shippingId.longValue())
                                                                        .map(v2 -> {
                                                                                logger.info("Successfully permanently deleted shipping address with ID: {}",
                                                                                                shippingId);
                                                                                return ApiResponse.<Void>success("Shipping address permanently deleted");
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAll() {
                logger.info("Restoring ALL trashed shipping addresses");

                return tracingMetrics.traceAndMeasure("restoreAllShippingAddresses", "restore_all_shipping_addresses",
                                () -> shippingAddressCommandRepository.restoreAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException("No trashed shipping addresses found");
                                                        }
                                                        logger.info("Successfully restored all trashed shipping addresses");
                                                        return ApiResponse.<Void>success("All shipping addresses restored successfully");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllPermanent() {
                logger.warn("Permanently deleting ALL trashed shipping addresses");

                return tracingMetrics.traceAndMeasure("deleteAllShippingAddressesPermanent",
                                "delete_all_shipping_addresses_permanent",
                                () -> shippingAddressCommandRepository.deleteAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException("No trashed shipping addresses found");
                                                        }
                                                        logger.info("Successfully permanently deleted all trashed shipping addresses");
                                                        return ApiResponse.<Void>success("All shipping addresses permanently deleted");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<ShippingAddressResponse>> create(
                        pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest req) {
                logger.info("Creating shipping address for orderId={}", req.getOrderId());

                return tracingMetrics.traceAndMeasure("createShippingAddress", "create_shipping_address",
                                Attributes.builder().put("order.id", String.valueOf(req.getOrderId())).build(),
                                () -> {
                                        ShippingAddress address = new ShippingAddress();
                                        address.setOrderId(req.getOrderId());
                                        address.setAlamat(req.getAlamat());
                                        address.setProvinsi(req.getProvinsi());
                                        address.setKota(req.getKota());
                                        address.setCourier(req.getCourier());
                                        address.setShippingMethod(req.getShippingMethod());
                                        address.setShippingCost(req.getShippingCost());
                                        address.setNegara(req.getNegara());

                                        return shippingAddressCommandRepository.persist(address)
                                                        .map(persisted -> {
                                                                logger.info("Successfully created shipping address for orderId={}",
                                                                                req.getOrderId());
                                                                return ApiResponse.success(
                                                                                "Shipping address created successfully",
                                                                                ShippingAddressResponse
                                                                                                .from(persisted));
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<ShippingAddressResponse>> update(
                        pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest req) {
                logger.info("Updating shipping address id={}", req.getShippingId());

                return tracingMetrics.traceAndMeasure("updateShippingAddress", "update_shipping_address",
                                Attributes.builder().put("shipping.id", String.valueOf(req.getShippingId())).build(),
                                () -> shippingAddressCommandRepository.findById((long) req.getShippingId())
                                                .chain(address -> {
                                                        if (address == null) {
                                                                throw new ResourceNotFoundException(
                                                                                "Shipping address not found with id="
                                                                                                + req.getShippingId());
                                                        }
                                                        address.setOrderId(req.getOrderId());
                                                        address.setAlamat(req.getAlamat());
                                                        address.setProvinsi(req.getProvinsi());
                                                        address.setKota(req.getKota());
                                                        address.setCourier(req.getCourier());
                                                        address.setShippingMethod(req.getShippingMethod());
                                                        address.setShippingCost(req.getShippingCost());
                                                        address.setNegara(req.getNegara());

                                                        return shippingAddressCommandRepository.persist(address)
                                                                        .chain(persisted -> invalidateCache(
                                                                                        (long) req.getShippingId())
                                                                                        .map(v -> {
                                                                                                logger.info("Successfully updated shipping address with ID: {}",
                                                                                                                req.getShippingId());
                                                                                                return ApiResponse
                                                                                                                .success("Shipping address updated successfully",
                                                                                                                                ShippingAddressResponse
                                                                                                                                                .from(persisted));
                                                                                        }));
                                                }));
        }
}