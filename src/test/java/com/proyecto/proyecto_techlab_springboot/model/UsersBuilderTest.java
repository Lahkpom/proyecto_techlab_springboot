package com.proyecto.proyecto_techlab_springboot.model;

import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UsersBuilderTest {

    @Test
    void defaultsFailedLoginAttemptsToZero() {
        Users user = Users.builder().build();

        assertEquals(0, user.getFailedLoginAttempts());
        assertFalse(user.isMustChangePassword());
    }
}