package com.tecsup.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void lasCredencialesInvalidasResponden401() {
        ResponseEntity<?> response =
            handler.handleBadCredentials(new BadCredentialsException("Credenciales invalidas"));

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void unRolInvalidoResponde400() {
        ResponseEntity<?> response =
            handler.handleIllegalArgument(new IllegalArgumentException("Rol no valido: TEST"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
