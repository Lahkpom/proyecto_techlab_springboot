package com.proyecto.proyecto_techlab_springboot.shipping.service;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.exception.InvalidShippingDestinationException;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingMethod;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingZone;
import com.proyecto.proyecto_techlab_springboot.shipping.repository.IShippingZoneRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingServiceTest {

    @Test
    void quotesPickupWithoutDestinationAndAllowsCashPayment() {
        IShippingZoneRepository repository = mock(IShippingZoneRepository.class);
        ShippingZone pickupZone = shippingZone(ShippingMethod.RETIRO_LLAVALLOL, "Retiro", "En el día");
        when(repository.findFirstByShippingMethodAndRowStatus(
                ShippingMethod.RETIRO_LLAVALLOL, RowStatus.ACTIVE
        ))
                .thenReturn(Optional.of(pickupZone));

        ShippingQuoteResponse result = new ShippingService(repository)
                .quoteShipping(true, null, null, null);

        assertEquals(ShippingMethod.RETIRO_LLAVALLOL, result.shippingMethod());
        assertEquals(new BigDecimal("0.00"), result.shippingCost());
        assertTrue(result.cashPaymentAllowed());
        verify(repository).findFirstByShippingMethodAndRowStatus(
                ShippingMethod.RETIRO_LLAVALLOL, RowStatus.ACTIVE
        );
    }

    @Test
    void quotesMatchedGbaPostalCodeAndAllowsCashPayment() {
        IShippingZoneRepository repository = mock(IShippingZoneRepository.class);
        ShippingZone zone = shippingZone(ShippingMethod.ENVIO_GBA_ZONA_SUR, "Zona Sur", "1 a 2 días");
        when(repository.findActiveZoneByPostalCode("[\"1836\"]")).thenReturn(Optional.of(zone));

        ShippingQuoteResponse result = new ShippingService(repository)
                .quoteShipping(false, "Buenos Aires", "Llavallol", " 1836 ");

        assertEquals(ShippingMethod.ENVIO_GBA_ZONA_SUR, result.shippingMethod());
        assertEquals(new BigDecimal("3000.00"), result.shippingCost());
        assertTrue(result.cashPaymentAllowed());
        verify(repository).findActiveZoneByPostalCode("[\"1836\"]");
    }

    @Test
    void fallsBackToNationalShippingWhenPostalCodeIsOutsideGba() {
        IShippingZoneRepository repository = mock(IShippingZoneRepository.class);
        ShippingZone nationalZone = shippingZone(
                ShippingMethod.ENVIO_CORREO_NACIONAL,
                "Envío Nacional",
                "3 a 7 días"
        );
        when(repository.findActiveZoneByPostalCode("[\"5000\"]")).thenReturn(Optional.empty());
        when(repository.findFirstByShippingMethodAndRowStatus(
                ShippingMethod.ENVIO_CORREO_NACIONAL,
                RowStatus.ACTIVE
        )).thenReturn(Optional.of(nationalZone));

        ShippingQuoteResponse result = new ShippingService(repository)
                .quoteShipping(false, "Córdoba", "Córdoba", "5000");

        assertEquals(ShippingMethod.ENVIO_CORREO_NACIONAL, result.shippingMethod());
        assertEquals(new BigDecimal("10000.00"), result.shippingCost());
        assertFalse(result.cashPaymentAllowed());
    }

    @Test
    void rejectsInvalidDestinationBeforeLookingUpZones() {
        IShippingZoneRepository repository = mock(IShippingZoneRepository.class);
        ShippingService service = new ShippingService(repository);

        assertThrows(
                InvalidShippingDestinationException.class,
                () -> service.quoteShipping(false, "Buenos Aires", "Llavallol", "1836A")
        );
        assertThrows(
                InvalidShippingDestinationException.class,
                () -> service.quoteShipping(false, " ", "Llavallol", "1836")
        );
        verifyNoInteractions(repository);
    }

    private ShippingZone shippingZone(ShippingMethod method, String name, String estimatedDays) {
        ShippingZone zone = mock(ShippingZone.class);
        when(zone.getShippingMethod()).thenReturn(method);
        when(zone.getZoneName()).thenReturn(name);
        when(zone.getDescription()).thenReturn(null);
        when(zone.getBaseCost()).thenReturn(switch (method) {
            case RETIRO_LLAVALLOL -> new BigDecimal("0.00");
            case ENVIO_GBA_ZONA_SUR, ENVIO_GBA_GENERAL -> new BigDecimal("3000.00");
            case ENVIO_CORREO_NACIONAL -> new BigDecimal("10000.00");
        });
        when(zone.getEstimatedDays()).thenReturn(estimatedDays);
        return zone;
    }
}
