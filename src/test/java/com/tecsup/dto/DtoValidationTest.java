package com.tecsup.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void unLoginConEmailInvalidoNoPasaLaValidacion() {
        LoginRequest request = new LoginRequest();
        request.setEmail("no-es-un-email");
        request.setPassword("123456");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void unLoginCompletoPasaLaValidacion() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("123456");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void unSignupConPasswordCortoNoPasaLaValidacion() {
        SignupRequest request = new SignupRequest();
        request.setUsername("user1");
        request.setEmail("user@test.com");
        request.setPassword("123");

        Set<ConstraintViolation<SignupRequest>> errores = validator.validate(request);

        assertFalse(errores.isEmpty());
    }
}
