package com.tecsup.jwt;

import com.tecsup.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthFilterTest {
    private JwtUtils jwtUtils;
    private JwtAuthFilter filter;
    private String token;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
            "clave_de_prueba_super_segura_para_tests_2026");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000);

        UserDetailsImpl user = new UserDetailsImpl(1L, "user1", "user@test.com", "encoded",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserDetailsService userDetailsService = email -> {
            if (!"user@test.com".equals(email)) {
                throw new UsernameNotFoundException("Usuario no encontrado: " + email);
            }
            return user;
        };

        filter = new JwtAuthFilter(jwtUtils, userDetailsService);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());
        token = jwtUtils.generateJwtToken(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaLaPeticionConUnTokenValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@test.com",
            SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void noAutenticaLaPeticionSinToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void noAutenticaLaPeticionConUnTokenInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token.invalido.aqui");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void noAutenticaSinElPrefijoBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
