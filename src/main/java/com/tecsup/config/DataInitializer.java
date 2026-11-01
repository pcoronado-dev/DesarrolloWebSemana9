package com.tecsup.config;

import com.tecsup.entity.Role;
import com.tecsup.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            createRoleIfMissing(roleRepository, "USER");
            createRoleIfMissing(roleRepository, "ADMIN");
            createRoleIfMissing(roleRepository, "MODERATOR");
        };
    }

    private void createRoleIfMissing(RoleRepository repository, String name) {
        if (repository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            repository.save(role);
        }
    }
}
