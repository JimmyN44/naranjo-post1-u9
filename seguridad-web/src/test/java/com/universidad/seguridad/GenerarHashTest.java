package com.universidad.seguridad;

import com.universidad.seguridad.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilidad de la guia (Parte 1, Paso 8) para generar el hash BCrypt del ADMIN.
 *
 * A diferencia de la guia, la contrasena NO se escribe en el codigo: se pasa
 * como propiedad del sistema y la prueba solo se ejecuta cuando se indica:
 *
 *   ./mvnw test -Dtest=GenerarHashTest -Dpassword='<contrasena>'
 *
 * Usa el mismo PasswordEncoder (BCrypt factor 12) definido en SecurityConfig.
 */
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
