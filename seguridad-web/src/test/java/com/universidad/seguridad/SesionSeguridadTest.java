package com.universidad.seguridad;

import com.universidad.seguridad.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SesionSeguridadTest extends IntegracionBase {

    @Nested
    @DisplayName("Registro y BCrypt")
    class Registro {

        @Test
        void registraUsuarioConHashBCrypt12YRolUser() throws Exception {
            mvc.perform(post("/registro").with(csrf())
                            .param("nombre", "Nuevo Usuario")
                            .param("email", "nuevo@universidad.edu")
                            .param("contrasenia", "Clave-Nueva-123"))
                    .andExpect(redirectedUrl("/login?registrado"));

            Usuario guardado = usuarioRepo.findByEmail("nuevo@universidad.edu").orElseThrow();
            assertThat(guardado.getContrasenia())
                    .startsWith("$2a$12$")
                    .isNotEqualTo("Clave-Nueva-123");
            assertThat(encoder.matches("Clave-Nueva-123", guardado.getContrasenia())).isTrue();
            assertThat(guardado.getRol()).isEqualTo(Usuario.ROL_USER);
        }

        @Test
        void ignoraCamposOcultosRolEIdMassAssignment() throws Exception {
            mvc.perform(post("/registro").with(csrf())
                            .param("id", String.valueOf(admin.getId()))
                            .param("rol", "ROLE_ADMIN")
                            .param("nombre", "Atacante")
                            .param("email", "atacante@universidad.edu")
                            .param("contrasenia", "Clave-Atacante-1"))
                    .andExpect(redirectedUrl("/login?registrado"));

            assertThat(usuarioRepo.findByEmail("atacante@universidad.edu").orElseThrow().getRol())
                    .isEqualTo(Usuario.ROL_USER);
            assertThat(usuarioRepo.findById(admin.getId()).orElseThrow().getEmail()).isEqualTo(ADMIN_EMAIL);
        }

        @Test
        void rechazaCorreoDuplicado() throws Exception {
            mvc.perform(post("/registro").with(csrf())
                            .param("nombre", "Duplicado")
                            .param("email", USER_EMAIL)
                            .param("contrasenia", "Otra-Clave-123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/registro"))
                    .andExpect(model().attributeHasFieldErrors("registro", "email"));
        }

        @Test
        void rechazaDatosInvalidos() throws Exception {
            mvc.perform(post("/registro").with(csrf())
                            .param("nombre", "")
                            .param("email", "no-es-correo")
                            .param("contrasenia", "corta"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("registro", "nombre", "email", "contrasenia"));
        }
    }

    @Nested
    @DisplayName("Login por sesion")
    class Login {

        @Test
        void rutaProtegidaSinSesionRedirigeALogin() throws Exception {
            mvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }

        @Test
        void loginCorrectoContraLaBDRedirigeAlDashboard() throws Exception {
            mvc.perform(formLogin("/login").user(USER_EMAIL).password(USER_PASS))
                    .andExpect(authenticated().withUsername(USER_EMAIL).withRoles("USER"))
                    .andExpect(redirectedUrl("/dashboard"));
        }

        @Test
        void loginIncorrectoRedirigeConError() throws Exception {
            mvc.perform(formLogin("/login").user(USER_EMAIL).password("incorrecta"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error=true"));
        }

        @Test
        void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
            user.setActivo(false);
            usuarioRepo.save(user);
            mvc.perform(formLogin("/login").user(USER_EMAIL).password(USER_PASS))
                    .andExpect(unauthenticated());
        }
    }

    @Nested
    @DisplayName("Autorizacion por roles")
    class Roles {

        @Test
        void userRecibe403EnPanelAdmin() throws Exception {
            mvc.perform(get("/admin").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminVeElPanelConLaListaDeUsuarios() throws Exception {
            mvc.perform(get("/admin").session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(USER_EMAIL)))
                    .andExpect(content().string(containsString(OTRO_EMAIL)));
        }

        @Test
        void enlaceAdminSoloVisibleParaAdmin() throws Exception {
            mvc.perform(get("/dashboard").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(content().string(not(containsString("Panel de Administración"))));
            mvc.perform(get("/dashboard").session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(content().string(containsString("Panel de Administración")));
        }
    }

    @Nested
    @DisplayName("Tareas: aislamiento entre usuarios")
    class Tareas {

        @Test
        void userSoloVeSusTareas() throws Exception {
            mvc.perform(get("/tareas").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Estudiar Spring Security")))
                    .andExpect(content().string(not(containsString("Tarea privada de user2"))));
        }

        @Test
        void adminVeTodasLasTareas() throws Exception {
            mvc.perform(get("/tareas").session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(content().string(containsString("Estudiar Spring Security")))
                    .andExpect(content().string(containsString("Tarea privada de user2")));
        }

        @Test
        void userCreaTareaPropia() throws Exception {
            mvc.perform(post("/tareas").with(csrf()).session(loginSesion(USER_EMAIL, USER_PASS))
                            .param("titulo", "Nueva tarea web").param("descripcion", "desc"))
                    .andExpect(redirectedUrl("/tareas"));
            assertThat(tareaRepo.findByPropietarioId(user.getId()))
                    .extracting("titulo").contains("Nueva tarea web");
        }

        @Test
        void userNoPuedeModificarNiEliminarTareaAjena() throws Exception {
            MockHttpSession s = loginSesion(USER_EMAIL, USER_PASS);
            mvc.perform(post("/tareas/{id}/completar", tareaOtro.getId()).with(csrf()).session(s))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/tareas/{id}/eliminar", tareaOtro.getId()).with(csrf()).session(s))
                    .andExpect(status().isForbidden());
            assertThat(tareaRepo.findById(tareaOtro.getId())).get()
                    .extracting("completada").isEqualTo(false);
        }

        @Test
        void userCompletaSuPropiaTarea() throws Exception {
            mvc.perform(post("/tareas/{id}/completar", tareaUser.getId()).with(csrf())
                            .session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(redirectedUrl("/tareas"));
            assertThat(tareaRepo.findById(tareaUser.getId()).orElseThrow().isCompletada()).isTrue();
        }

        @Test
        void adminPuedeEliminarCualquierTarea() throws Exception {
            mvc.perform(post("/tareas/{id}/eliminar", tareaOtro.getId()).with(csrf())
                            .param("origen", "admin")
                            .session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(redirectedUrl("/admin"));
            assertThat(tareaRepo.findById(tareaOtro.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("XSS: th:text escapa el contenido")
    class Xss {

        private static final String PAYLOAD = "<script>alert(\"XSS\")</script>";

        @Test
        void nombreConScriptSeMuestraEscapadoEnElDashboard() throws Exception {
            mvc.perform(post("/registro").with(csrf())
                            .param("nombre", PAYLOAD)
                            .param("email", "xss@universidad.edu")
                            .param("contrasenia", "Clave-Xss-1234"))
                    .andExpect(redirectedUrl("/login?registrado"));

            mvc.perform(get("/dashboard").session(loginSesion("xss@universidad.edu", "Clave-Xss-1234")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(
                            "&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;")))
                    .andExpect(content().string(not(containsString(PAYLOAD))))
                    // Ninguna etiqueta <script> llega al navegador (ni en comentarios)
                    .andExpect(content().string(not(containsString("<script"))));

            // El panel de administracion tambien escapa el nombre
            mvc.perform(get("/admin").session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(content().string(containsString("&lt;script&gt;")))
                    .andExpect(content().string(not(containsString("<script"))));
        }

        @Test
        void tituloDeTareaConScriptSeMuestraEscapado() throws Exception {
            MockHttpSession s = loginSesion(USER_EMAIL, USER_PASS);
            mvc.perform(post("/tareas").with(csrf()).session(s).param("titulo", PAYLOAD))
                    .andExpect(redirectedUrl("/tareas"));
            mvc.perform(get("/tareas").session(s))
                    .andExpect(content().string(containsString("&lt;script&gt;")))
                    .andExpect(content().string(not(containsString("<script"))));
        }

        @Test
        void ningunaPlantillaUsaThUtext() throws Exception {
            var plantillas = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                    .getResources("classpath:templates/**/*.html");
            assertThat(plantillas).isNotEmpty();
            for (var p : plantillas) {
                String fuente = p.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                assertThat(fuente).as(p.getFilename()).doesNotContain("th:utext");
            }
        }
    }

    @Nested
    @DisplayName("SQL Injection: @Query parametrizado")
    class SqlInjection {

        private static final String INYECCION = "' OR '1'='1";

        @Test
        void repositorioTrataLaEntradaComoLiteral() {
            assertThat(tareaRepo.count()).isEqualTo(2);
            assertThat(tareaRepo.buscarPorTitulo(INYECCION)).isEmpty();
            assertThat(tareaRepo.buscarPorTituloYPropietario(INYECCION, user.getId())).isEmpty();
            assertThat(tareaRepo.buscarPorTitulo("Spring")).hasSize(1);
        }

        @Test
        void buscadorWebDevuelveCeroResultadosConLaInyeccion() throws Exception {
            mvc.perform(get("/tareas").param("q", INYECCION).session(loginSesion(ADMIN_EMAIL, ADMIN_PASS)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("tareas", org.hamcrest.Matchers.empty()));
        }

        @Test
        void busquedaLegitimaSigueFuncionando() throws Exception {
            mvc.perform(get("/tareas").param("q", "Spring").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(model().attribute("tareas", org.hamcrest.Matchers.hasSize(1)));
        }

        @Test
        void userNoEncuentraTareasAjenasConElBuscador() throws Exception {
            mvc.perform(get("/tareas").param("q", "privada").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(model().attribute("tareas", org.hamcrest.Matchers.empty()));
        }
    }

    @Nested
    @DisplayName("CSRF activo en las vistas web")
    class Csrf {

        @Test
        void logoutSinTokenCsrfDevuelve403() throws Exception {
            mvc.perform(post("/logout").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void logoutConTokenCsrfRedirigeALogin() throws Exception {
            mvc.perform(post("/logout").with(csrf()).session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(redirectedUrl("/login?logout"))
                    .andExpect(unauthenticated());
        }

        @Test
        void formulariosSinTokenCsrfDevuelven403() throws Exception {
            MockHttpSession s = loginSesion(USER_EMAIL, USER_PASS);
            mvc.perform(post("/tareas").session(s).param("titulo", "sin token"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/registro").param("nombre", "x").param("email", "x@universidad.edu")
                            .param("contrasenia", "Clave-12345"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/login").param("username", USER_EMAIL).param("password", USER_PASS))
                    .andExpect(status().isForbidden());
        }

        @Test
        void thymeleafIncluyeElTokenCsrfEnLosFormularios() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(content().string(containsString("name=\"_csrf\"")));
        }
    }
}
