package com.tecsup.jwt;

import com.tecsup.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {
    private static final String SECRET = "clave_de_prueba_super_segura_para_tests_2026";

    private JwtUtils jwtUtils;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        jwtUtils = crearJwtUtils(SECRET, 3_600_000);

        UserDetailsImpl user = new UserDetailsImpl(1L, "user1", "user@test.com", "encoded",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());
    }

    @Test
    void generaUnTokenValidoConElEmailComoSubject() {
        String token = jwtUtils.generateJwtToken(authentication);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals("user@test.com", jwtUtils.getUserNameFromJwtToken(token));
        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void rechazaUnTokenConLaFirmaAlterada() {
        String token = jwtUtils.generateJwtToken(authentication);
        String[] partes = token.split("\\.");
        char[] payload = partes[1].toCharArray();
        payload[0] = payload[0] == 'A' ? 'B' : 'A';

        String alterado = partes[0] + "." + new String(payload) + "." + partes[2];

        assertFalse(jwtUtils.validateJwtToken(alterado));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        JwtUtils otroEmisor = crearJwtUtils(
            "otra_clave_totalmente_distinta_para_tests_2026", 3_600_000);

        String token = otroEmisor.generateJwtToken(authentication);

        assertFalse(jwtUtils.validateJwtToken(token));
    }

    @Test
    void rechazaUnTokenExpirado() {
        JwtUtils expirado = crearJwtUtils(SECRET, -1_000);

        String token = expirado.generateJwtToken(authentication);

        assertFalse(jwtUtils.validateJwtToken(token));
    }

    @Test
    void rechazaUnTextoQueNoEsUnJwt() {
        assertFalse(jwtUtils.validateJwtToken("esto-no-es-un-jwt"));
        assertFalse(jwtUtils.validateJwtToken(null));
    }

    private JwtUtils crearJwtUtils(String secret, int expirationMs) {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        ReflectionTestUtils.setField(utils, "jwtExpirationMs", expirationMs);
        return utils;
    }
}
