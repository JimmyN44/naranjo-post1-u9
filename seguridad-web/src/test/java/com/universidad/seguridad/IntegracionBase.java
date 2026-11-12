package com.universidad.seguridad;

import com.universidad.seguridad.model.Tarea;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.repository.TareaRepository;
import com.universidad.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;


@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegracionBase {

    static final String JWT_SECRET_PRUEBA = JwtUtilTest.claveAleatoria();

    static final String ADMIN_EMAIL = "admin@universidad.edu";
    static final String ADMIN_PASS = "Admin-Prueba-123";
    static final String USER_EMAIL = "user1@universidad.edu";
    static final String USER_PASS = "User1-Prueba-123";
    static final String OTRO_EMAIL = "user2@universidad.edu";
    static final String OTRO_PASS = "User2-Prueba-123";

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET_PRUEBA);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarioRepo;
    @Autowired protected TareaRepository tareaRepo;
    @Autowired protected PasswordEncoder encoder;

    protected Usuario admin;
    protected Usuario user;
    protected Usuario otro;
    protected Tarea tareaUser;
    protected Tarea tareaOtro;

    @BeforeEach
    void prepararDatos() {
        tareaRepo.deleteAll();
        usuarioRepo.deleteAll();
        admin = usuarioRepo.save(new Usuario("Administrador", ADMIN_EMAIL, encoder.encode(ADMIN_PASS), Usuario.ROL_ADMIN));
        user = usuarioRepo.save(new Usuario("Usuario Uno", USER_EMAIL, encoder.encode(USER_PASS), Usuario.ROL_USER));
        otro = usuarioRepo.save(new Usuario("Usuario Dos", OTRO_EMAIL, encoder.encode(OTRO_PASS), Usuario.ROL_USER));
        tareaUser = tareaRepo.save(new Tarea("Estudiar Spring Security", "Parte 1", user));
        tareaOtro = tareaRepo.save(new Tarea("Tarea privada de user2", "No visible para user1", otro));
    }

    protected MockHttpSession loginSesion(String email, String pass) throws Exception {
        MvcResult r = mvc.perform(formLogin("/login").user(email).password(pass))
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }
}
