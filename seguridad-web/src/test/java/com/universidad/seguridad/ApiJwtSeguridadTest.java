package com.universidad.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.seguridad.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiJwtSeguridadTest extends IntegracionBase {

    @Autowired private ObjectMapper mapper;

    private String loginApi(String email, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Credenciales(email, pass))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    record Credenciales(String email, String contrasenia) {}

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        void credencialesValidasDevuelvenTokenEmailYRol() throws Exception {
            MvcResult r = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new Credenciales(ADMIN_EMAIL, ADMIN_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                    .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                    .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"))
                    .andReturn();
            JsonNode body = mapper.readTree(r.getResponse().getContentAsString());
            assertThat(body.get("token").asText().split("\\.")).hasSize(3);
        }

        @Test
        void contraseniaIncorrectaDevuelve401SinToken() throws Exception {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new Credenciales(ADMIN_EMAIL, "incorrecta"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        void usuarioInexistenteDevuelve401() throws Exception {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new Credenciales("nadie@universidad.edu", "x"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void loginNoRequiereTokenCsrfNiCreaSesion() throws Exception {
            MvcResult r = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new Credenciales(USER_EMAIL, USER_PASS))))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(r.getRequest().getSession(false)).isNull();
            assertThat(r.getResponse().getHeader("Set-Cookie")).isNull();
        }
    }

    @Nested
    @DisplayName("Validacion del token en /api/**")
    class Token {

        @Test
        void tokenValidoEsAceptadoYDevuelveSoloTareasPropias() throws Exception {
            mvc.perform(get("/api/tareas").header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].titulo").value("Estudiar Spring Security"))
                    .andExpect(jsonPath("$[0].propietarioEmail").value(USER_EMAIL));
        }

        @Test
        void sinTokenDevuelve401() throws Exception {
            mvc.perform(get("/api/tareas"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void tokenConFirmaModificadaDevuelve401() throws Exception {
            String alterado = JwtUtilTest.alterarFirma(loginApi(USER_EMAIL, USER_PASS));
            mvc.perform(get("/api/tareas").header("Authorization", bearer(alterado)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenMalformadoDevuelve401() throws Exception {
            mvc.perform(get("/api/tareas").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.token.modificado"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenExpiradoDevuelve401() throws Exception {
            String expirado = new JwtUtil(JWT_SECRET_PRUEBA, -1_000).generarToken(USER_EMAIL, "ROLE_USER");
            mvc.perform(get("/api/tareas").header("Authorization", bearer(expirado)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenFirmadoConOtraClaveDevuelve401() throws Exception {
            String ajeno = new JwtUtil(JwtUtilTest.claveAleatoria(), 60_000)
                    .generarToken(ADMIN_EMAIL, "ROLE_ADMIN");
            mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(ajeno)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void esquemaDistintoDeBearerDevuelve401() throws Exception {
            mvc.perform(get("/api/tareas").header("Authorization", "Basic " + loginApi(USER_EMAIL, USER_PASS)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void peticionAutenticadaNoCreaSesion() throws Exception {
            MvcResult r = mvc.perform(get("/api/tareas").header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS))))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(r.getRequest().getSession(false)).isNull();
        }
    }

    @Nested
    @DisplayName("Autorizacion por roles y propiedad en la API")
    class Roles {

        @Test
        void userRecibe403EnRutasAdmin() throws Exception {
            String token = loginApi(USER_EMAIL, USER_PASS);
            mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/tareas").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminAccedeAUsuariosSinExponerHashes() throws Exception {
            MvcResult r = mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(loginApi(ADMIN_EMAIL, ADMIN_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andReturn();
            String json = r.getResponse().getContentAsString();
            assertThat(json).doesNotContain("contrasenia").doesNotContain("$2a$");
        }

        @Test
        void adminVeTodasLasTareas() throws Exception {
            mvc.perform(get("/api/admin/tareas").header("Authorization", bearer(loginApi(ADMIN_EMAIL, ADMIN_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void tareasNoExponenElHashDelPropietario() throws Exception {
            MvcResult r = mvc.perform(get("/api/tareas").header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS))))
                    .andReturn();
            assertThat(r.getResponse().getContentAsString()).doesNotContain("$2a$").doesNotContain("contrasenia");
        }

        @Test
        void userCreaTareaSinCsrfPorqueLaApiEsStateless() throws Exception {
            mvc.perform(post("/api/tareas")
                            .header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"Tarea via API\",\"descripcion\":\"JWT\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.propietarioEmail").value(USER_EMAIL));
        }

        @Test
        void userNoPuedeModificarNiEliminarTareaAjena() throws Exception {
            String token = loginApi(USER_EMAIL, USER_PASS);
            mvc.perform(patch("/api/tareas/{id}/completar", tareaOtro.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/tareas/{id}", tareaOtro.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            assertThat(tareaRepo.findById(tareaOtro.getId())).isPresent();
        }

        @Test
        void adminPuedeEliminarTareaDeCualquierUsuario() throws Exception {
            mvc.perform(delete("/api/tareas/{id}", tareaOtro.getId())
                            .header("Authorization", bearer(loginApi(ADMIN_EMAIL, ADMIN_PASS))))
                    .andExpect(status().isNoContent());
            assertThat(tareaRepo.findById(tareaOtro.getId())).isEmpty();
        }

        @Test
        void busquedaApiResisteInyeccion() throws Exception {
            mvc.perform(get("/api/tareas/buscar").param("texto", "' OR '1'='1")
                            .header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Coexistencia de las dos SecurityFilterChain")
    class Coexistencia {

        @Test
        void sesionYJwtFuncionanSimultaneamente() throws Exception {
            MockHttpSession sesion = loginSesion(ADMIN_EMAIL, ADMIN_PASS);
            String token = loginApi(ADMIN_EMAIL, ADMIN_PASS);

            mvc.perform(get("/dashboard").session(sesion)).andExpect(status().isOk());
            mvc.perform(get("/api/tareas").header("Authorization", bearer(token))).andExpect(status().isOk());
            mvc.perform(get("/admin").session(sesion)).andExpect(status().isOk());
            mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token))).andExpect(status().isOk());
            mvc.perform(get("/dashboard").session(sesion)).andExpect(status().isOk());
        }

        @Test
        void laCookieDeSesionNoAutenticaEnLaApi() throws Exception {
            mvc.perform(get("/api/tareas").session(loginSesion(USER_EMAIL, USER_PASS)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void elJwtNoAutenticaLasVistasWeb() throws Exception {
            mvc.perform(get("/dashboard").header("Authorization", bearer(loginApi(USER_EMAIL, USER_PASS))))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }

        @Test
        void csrfSigueActivoEnLaWebMientrasEstaDesactivadoEnLaApi() throws Exception {
            String token = loginApi(USER_EMAIL, USER_PASS);
            mvc.perform(post("/api/tareas").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"api\"}"))
                    .andExpect(status().isCreated());
            mvc.perform(post("/tareas").session(loginSesion(USER_EMAIL, USER_PASS)).param("titulo", "web"))
                    .andExpect(status().isForbidden());
        }
    }
}
