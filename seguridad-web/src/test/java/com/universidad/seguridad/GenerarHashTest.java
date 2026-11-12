package com.universidad.seguridad;

import com.universidad.seguridad.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;


@EnabledIfSystemProperty(named = "password", matches = ".+")
class GenerarHashTest {

    @Test
    void generarHashAdmin() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String hash = encoder.encode(System.getProperty("password"));
        System.out.println("HASH BCRYPT: " + hash);
        assertTrue(hash.startsWith("$2a$12$"));
    }
}
