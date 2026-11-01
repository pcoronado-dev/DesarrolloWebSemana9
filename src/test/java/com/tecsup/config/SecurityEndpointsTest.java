package com.tecsup.config;

import com.tecsup.controller.TestController;
import com.tecsup.jwt.JwtUtils;
import com.tecsup.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, SecurityEndpointsTest.TestBeans.class})
class SecurityEndpointsTest {
    private static final String SECRET = "clave_de_prueba_super_segura_para_tests_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void elEndpointPublicoResponde200SinToken() throws Exception {
        mockMvc.perform(get("/api/test/all"))
            .andExpect(status().isOk())
            .andExpect(content().string("Public Content"));
    }

    @Test
    void unEndpointProtegidoResponde401SinToken() throws Exception {
        mockMvc.perform(get("/api/test/user"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Token ausente, invalido o expirado"));
    }

    @Test
    void unEndpointProtegidoResponde401ConTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/test/user")
                .header("Authorization", "Bearer token.invalido.aqui"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioConRolUserAccedeAlEndpointUser() throws Exception {
        mockMvc.perform(get("/api/test/user")
                .header("Authorization", "Bearer " + token("user@test.com")))
            .andExpect(status().isOk())
            .andExpect(content().string("User Content"));
    }

    @Test
    void unUsuarioConRolUserRecibe403EnElEndpointAdmin() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + token("user@test.com")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value("No tiene permisos para acceder a este recurso"));
    }

    @Test
    void unAdminAccedeAlEndpointAdmin() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + token("admin@test.com")))
            .andExpect(status().isOk())
            .andExpect(content().string("Admin Content"));
    }

    private String token(String email) {
        String role = email.startsWith("admin") ? "ADMIN" : "USER";
        UserDetailsImpl user = new UserDetailsImpl(1L, email, email, "encoded",
            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return jwtUtils.generateJwtToken(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JwtUtils jwtUtils() {
            JwtUtils jwtUtils = new JwtUtils();
            ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000);
            return jwtUtils;
        }

        @Bean
        UserDetailsService userDetailsService() {
            return email -> {
                String role = email.startsWith("admin") ? "ADMIN" : "USER";
                return new UserDetailsImpl(1L, email, email, "encoded",
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            };
        }
    }
}
