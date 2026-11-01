package com.tecsup.controller;

import com.tecsup.dto.JwtResponse;
import com.tecsup.dto.LoginRequest;
import com.tecsup.dto.MessageResponse;
import com.tecsup.dto.SignupRequest;
import com.tecsup.entity.Role;
import com.tecsup.entity.User;
import com.tecsup.jwt.JwtUtils;
import com.tecsup.repository.RoleRepository;
import com.tecsup.repository.UserRepository;
import com.tecsup.service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder encoder, AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().isBlank()
                || signUpRequest.getEmail() == null || signUpRequest.getEmail().isBlank()
                || signUpRequest.getPassword() == null || signUpRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username, email y password son obligatorios"));
        }

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username ya existe"));
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email ya existe"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(findRole("USER"));
        } else {
            for (String role : strRoles) {
                switch (role.toUpperCase()) {
                    case "ADMIN" -> roles.add(findRole("ADMIN"));
                    case "MODERATOR" -> roles.add(findRole("MODERATOR"));
                    case "USER" -> roles.add(findRole("USER"));
                    default -> throw new IllegalArgumentException("Rol no valido: " + role);
                }
            }
        }

        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Usuario registrado correctamente"));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
            userDetails.getId(), userDetails.getUsername(),
            userDetails.getEmail(), jwt, roles));
    }

    private Role findRole(String name) {
        return roleRepository.findByName(name)
            .orElseThrow(() -> new IllegalStateException("Rol " + name + " no encontrado"));
    }
}
