package com.universidad.seguridad.config;

import com.universidad.seguridad.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


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
            return; 
        }
        if (usuarioService.existeEmail(email)) {
            log.info("El usuario ADMIN {} ya existe; no se crea de nuevo", email);
            return;
        }
        usuarioService.crearAdmin("Administrador", email, password);
        log.info("Usuario ADMIN {} creado desde variables de entorno", email);
    }
}
