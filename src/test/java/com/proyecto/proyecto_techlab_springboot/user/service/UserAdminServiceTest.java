package com.proyecto.proyecto_techlab_springboot.user.service;

import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.exception.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.dto.AdminUserUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserAdminSegment;
import com.proyecto.proyecto_techlab_springboot.user.exception.InvalidUserDataException;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {

    @Test
    void operatorSearchIsConstrainedToCustomersAndWholesalers() {
        IUserRepository repository = mock(IUserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        AuthenticatedUserPrincipal operator =
                new AuthenticatedUserPrincipal(UUID.randomUUID(), "operator@example.com", UserRole.OPERATOR);
        Pageable pageable = Pageable.unpaged();
        when(repository.searchUsersAdmin(
                List.of(UserRole.CUSTOMER, UserRole.WHOLESALER),
                null,
                null,
                null,
                pageable
        )).thenReturn(Page.empty(pageable));

        Page<?> result = service.searchUsersAdmin(
                operator,
                UserAdminSegment.CLIENTS,
                null,
                null,
                null,
                pageable
        );

        assertEquals(0, result.getTotalElements());
        verify(repository).searchUsersAdmin(
                List.of(UserRole.CUSTOMER, UserRole.WHOLESALER),
                null,
                null,
                null,
                pageable
        );
    }

    @Test
    void operatorCannotSearchTheOperatorSegment() {
        IUserRepository repository = mock(IUserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        AuthenticatedUserPrincipal operator =
                new AuthenticatedUserPrincipal(UUID.randomUUID(), "operator@example.com", UserRole.OPERATOR);

        assertThrows(
                AccessDeniedException.class,
                () -> service.searchUsersAdmin(
                        operator,
                        UserAdminSegment.OPERATORS,
                        null,
                        null,
                        null,
                        Pageable.unpaged()
                )
        );
        verifyNoInteractions(repository);
    }

    @Test
    void wholesalerRoleRequiresAnElevenDigitCuit() {
        IUserRepository repository = mock(IUserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        UUID userId = UUID.randomUUID();
        Users customer = user(userId, UserRole.CUSTOMER);
        when(repository.findById(userId)).thenReturn(Optional.of(customer));
        AuthenticatedUserPrincipal admin =
                new AuthenticatedUserPrincipal(UUID.randomUUID(), "admin@example.com", UserRole.ADMIN);

        assertThrows(
                InvalidUserDataException.class,
                () -> service.updateAdminUser(
                        userId,
                        new AdminUserUpdateRequest(UserRole.WHOLESALER, "Compañía", TaxIdType.CUIL, "20123456789"),
                        admin
                )
        );
        assertEquals(UserRole.CUSTOMER, customer.getUserRole());
    }

    @Test
    void operatorCannotDeleteCustomerAccounts() {
        IUserRepository repository = mock(IUserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        UUID userId = UUID.randomUUID();
        Users customer = user(userId, UserRole.CUSTOMER);
        when(repository.findById(userId)).thenReturn(Optional.of(customer));
        AuthenticatedUserPrincipal operator =
                new AuthenticatedUserPrincipal(UUID.randomUUID(), "operator@example.com", UserRole.OPERATOR);

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateAdminUserStatus(
                        userId,
                        new StatusUpdateRequest(RowStatus.DELETED),
                        operator
                )
        );
        assertEquals(RowStatus.ACTIVE, customer.getRowStatus());
    }

    @Test
    void administratorDeletionAnonymizesTheAccount() {
        IUserRepository repository = mock(IUserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        UUID userId = UUID.randomUUID();
        Users customer = user(userId, UserRole.CUSTOMER);
        when(repository.findById(userId)).thenReturn(Optional.of(customer));
        AuthenticatedUserPrincipal admin =
                new AuthenticatedUserPrincipal(UUID.randomUUID(), "admin@example.com", UserRole.ADMIN);

        service.updateAdminUserStatus(
                userId,
                new StatusUpdateRequest(RowStatus.DELETED),
                admin
        );

        assertEquals(RowStatus.DELETED, customer.getRowStatus());
        assertEquals("deleted_" + userId + "@anonymized.local", customer.getUserEmail());
        assertEquals("Usuario Eliminado", customer.getUserName());
        assertEquals("ANONYMIZED", customer.getUserPassword());
    }

    private Users user(UUID userId, UserRole role) {
        return Users.builder()
                .userId(userId)
                .userRole(role)
                .userName("Test User")
                .userEmail("test@example.com")
                .userPassword("encoded-password")
                .rowStatus(RowStatus.ACTIVE)
                .build();
    }
}
