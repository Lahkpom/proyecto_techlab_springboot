package com.proyecto.proyecto_techlab_springboot.user.service;

import com.proyecto.proyecto_techlab_springboot.user.exception.InvalidUserDataException;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class UserServiceTaxIdentificationTest {

    private final UserService userService =
            new UserService(mock(IUserRepository.class), mock(PasswordEncoder.class));

    @Test
    void normalizesValidCustomerTaxIdentification() {
        assertEquals(
                "12345678",
                userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, TaxIdType.DNI, " 12345678 ")
        );
    }

    @Test
    void allowsCustomerWithoutTaxIdentification() {
        assertNull(userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, null, "  "));
    }

    @Test
    void rejectsTaxIdentificationWithoutItsTypeOrNumber() {
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, TaxIdType.CUIT, null)
        );
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, null, "20123456789")
        );
    }

    @Test
    void validatesNumberFormatForEachTaxIdentificationType() {
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, TaxIdType.DNI, "123456")
        );
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, TaxIdType.CUIL, "1234567890")
        );
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.CUSTOMER, TaxIdType.PASAPORTE, "ABC-12")
        );
    }

    @Test
    void requiresAnElevenDigitCuitForWholesalers() {
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.WHOLESALER, TaxIdType.CUIL, "20123456789")
        );
        assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateTaxIdentificationForRole(UserRole.WHOLESALER, TaxIdType.CUIT, "1234567890")
        );
        assertEquals(
                "20123456789",
                userService.validateTaxIdentificationForRole(UserRole.WHOLESALER, TaxIdType.CUIT, " 20123456789 ")
        );
    }
}
