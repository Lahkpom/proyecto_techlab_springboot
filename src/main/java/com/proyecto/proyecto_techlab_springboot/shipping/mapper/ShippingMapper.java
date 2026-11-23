package com.proyecto.proyecto_techlab_springboot.shipping.mapper;

import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingZone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShippingMapper {

    public ShippingQuoteResponse toQuoteResponse(
            ShippingZone zone,
            BigDecimal shippingCost,
            boolean cashPaymentAllowed
    ) {
        return new ShippingQuoteResponse(
                zone.getShippingMethod(),
                zone.getZoneName(),
                zone.getDescription(),
                shippingCost,
                zone.getEstimatedDays(),
                cashPaymentAllowed
        );
    }

    public ShippingZoneResponse toZoneResponse(ShippingZone zone) {
        return new ShippingZoneResponse(
                zone.getShippingZoneId(),
                zone.getShippingMethod(),
                zone.getZoneName(),
                zone.getDescription(),
                zone.getPostalCodes(),
                zone.getBaseCost(),
                zone.getEstimatedDays(),
                zone.getRowStatus()
        );
    }
}
