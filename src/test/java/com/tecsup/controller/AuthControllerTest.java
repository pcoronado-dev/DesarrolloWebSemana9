package com.tecsup.controller;

import com.tecsup.dto.JwtResponse;
import com.tecsup.dto.LoginRequest;
import com.tecsup.jwt.JwtUtils;
import com.tecsup.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthControllerTest {
    private AuthController controller;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
            "clave_de_prueba_super_segura_para_tests_2026");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000);

        UserDetailsImpl user = new UserDetailsImpl(1L, "user1", "user@test.com", "encoded",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

        AuthenticationManager authenticationManager = authentication -> {
            UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) authentication;

            if (!"user@test.com".equals(token.getPrincipal())
                    || !"123456".equals(token.getCredentials())) {
                throw new BadCredentialsException("Credenciales invalidas");
            }

            return new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        };

        controller = new AuthController(null, null, null, authenticationManager, jwtUtils);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void elLoginDevuelveElTokenYLosRoles() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("123456");

        ResponseEntity<?> response = controller.authenticateUser(request);

        assertEquals(200, response.getStatusCode().value());
        JwtResponse body = (JwtResponse) response.getBody();
        assertNotNull(body);
        assertFalse(body.getAccessToken().isBlank());
        assertEquals("Bearer", body.getTokenType());
        assertEquals("user@test.com", body.getEmail());
        assertEquals(List.of("ROLE_USER"), body.getRoles());
    }

    @Test
    void elLoginConPasswordIncorrectoLanzaBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("incorrecto");

        assertThrows(BadCredentialsException.class, () -> controller.authenticateUser(request));
    }
}
