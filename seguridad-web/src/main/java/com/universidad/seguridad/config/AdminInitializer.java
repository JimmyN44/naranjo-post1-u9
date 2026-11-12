package com.universidad.seguridad.config;

import com.universidad.seguridad.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Alternativa OPCIONAL al INSERT manual de la guia (Parte 1, Paso 8).
 *
 * Solo actua si se definen las variables de entorno ADMIN_EMAIL y
 * ADMIN_PASSWORD; en ese caso crea el usuario ROLE_ADMIN con la contrasena
 * hasheada por BCrypt, siempre que el correo no exista todavia.
 * Ninguna credencial queda escrita en el codigo fuente.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioService usuarioService;
    private final String email;
    private final String password;

    public AdminInitializer(UsuarioService usuarioService,
                            @Value("${app.admin.email:}") String email,
                            @Value("${app.admin.password:}") String password) {
        this.usuarioService = usuarioService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return; // se usa el procedimiento manual (docs/sql/admin.sql)
        }
        if (usuarioService.existeEmail(email)) {
            log.info("El usuario ADMIN {} ya existe; no se crea de nuevo", email);
            return;
        }
        usuarioService.crearAdmin("Administrador", email, password);
        log.info("Usuario ADMIN {} creado desde variables de entorno", email);
    }
}
