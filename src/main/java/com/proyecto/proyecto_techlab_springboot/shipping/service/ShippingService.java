package com.proyecto.proyecto_techlab_springboot.shipping.service;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteRequest;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.shipping.exception.InvalidShippingDestinationException;
import com.proyecto.proyecto_techlab_springboot.shipping.exception.ShippingZoneNotFoundException;
import com.proyecto.proyecto_techlab_springboot.shipping.mapper.ShippingMapper;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingMethod;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingZone;
import com.proyecto.proyecto_techlab_springboot.shipping.repository.IShippingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final BigDecimal PICKUP_COST = new BigDecimal("0.00");

    private final IShippingZoneRepository shippingZoneRepository;
    private final ShippingMapper shippingMapper;

    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> getShippingZones() {
        return shippingZoneRepository.findAllByRowStatusNotOrderByBaseCostAsc(RowStatus.DELETED)
                .stream()
                .map(this.shippingMapper::toZoneResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShippingZoneResponse getShippingZone(Long shippingZoneId) {
        return this.shippingMapper.toZoneResponse(findShippingZoneByIdOrThrow(shippingZoneId));
    }

    @Transactional
    public ShippingZoneResponse updateShippingZone(Long shippingZoneId, ShippingZoneUpdateRequest request) {
        ShippingZone zone = findShippingZoneByIdOrThrow(shippingZoneId);
        zone.setZoneName(request.zoneName().trim());
        zone.setDescription(request.description());
        zone.setBaseCost(request.baseCost());
        zone.setEstimatedDays(request.estimatedDays().trim());
        if (request.postalCodes() != null) {
            zone.setPostalCodes(List.copyOf(request.postalCodes()));
        }
        if (request.rowStatus() != null) {
            zone.setRowStatus(request.rowStatus());
        }

        return this.shippingMapper.toZoneResponse(zone);
    }

    @Transactional
    public ShippingZoneResponse updateShippingZoneStatus(Long shippingZoneId, StatusUpdateRequest request) {
        ShippingZone zone = findShippingZoneByIdOrThrow(shippingZoneId);
        zone.setRowStatus(request.rowStatus());
        return this.shippingMapper.toZoneResponse(zone);
    }

    private ShippingZone findShippingZoneByIdOrThrow(Long shippingZoneId) {
        return shippingZoneRepository.findById(shippingZoneId)
                .orElseThrow(() -> new ShippingZoneNotFoundException("{ ID: %s }".formatted(shippingZoneId)));
    }

    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> getActiveShippingOptions() {
        return shippingZoneRepository.findAllByRowStatusOrderByBaseCostAsc(RowStatus.ACTIVE)
                .stream()
                .map(this.shippingMapper::toZoneResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShippingQuoteResponse quoteShipping(
            boolean isPickup,
            String province,
            String locality,
            String postalCode
    ) {
        if (isPickup) {
            ShippingZone pickupZone = findActiveZoneByMethod(ShippingMethod.RETIRO_LLAVALLOL);
            return this.shippingMapper.toQuoteResponse(pickupZone, PICKUP_COST, true);
        }

        String normalizedCp = normalizeDestination(province, locality, postalCode);
        ShippingZone zone = shippingZoneRepository
                .findActiveZoneByPostalCode("[\"%s\"]".formatted(normalizedCp))
                .filter(this::isGbaZone)
                .orElseGet(() -> findActiveZoneByMethod(ShippingMethod.ENVIO_CORREO_NACIONAL));

        return this.shippingMapper.toQuoteResponse(
                zone,
                zone.getBaseCost(),
                isGbaZone(zone)
        );
    }

    @Transactional(readOnly = true)
    public ShippingQuoteResponse quoteShipping(ShippingQuoteRequest request) {
        return quoteShipping(request.isPickup(), request.province(), request.locality(), request.postalCode());
    }

    private String normalizeDestination(String province, String locality, String postalCode) {
        if (province == null || province.isBlank()) {
            throw new InvalidShippingDestinationException("La provincia de destino es obligatoria.");
        }
        if (locality == null || locality.isBlank()) {
            throw new InvalidShippingDestinationException("La localidad de destino es obligatoria.");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new InvalidShippingDestinationException("El código postal de destino es obligatorio.");
        }

        String normalizedCp = postalCode.trim();
        if (!normalizedCp.matches("^\\d{4}$")) {
            throw new InvalidShippingDestinationException("El código postal debe contener cuatro dígitos.");
        }

        return normalizedCp;
    }

    private ShippingZone findActiveZoneByMethod(ShippingMethod method) {
        return shippingZoneRepository.findFirstByShippingMethodAndRowStatus(method, RowStatus.ACTIVE)
                .orElseThrow(() -> new ShippingZoneNotFoundException("{ shippingMethod: %s }".formatted(method)));
    }

    private boolean isGbaZone(ShippingZone zone) {
        return zone.getShippingMethod() == ShippingMethod.ENVIO_GBA_ZONA_SUR
                || zone.getShippingMethod() == ShippingMethod.ENVIO_GBA_GENERAL;
    }

}
