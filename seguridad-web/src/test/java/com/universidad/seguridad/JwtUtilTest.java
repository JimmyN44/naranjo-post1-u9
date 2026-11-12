package com.universidad.seguridad;

import com.universidad.seguridad.security.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String CLAVE_A = claveAleatoria();
    private static final String CLAVE_B = claveAleatoria();

    static String claveAleatoria() {
        byte[] bytes = new byte[48];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void generaTokenConEmailYRol() {
        JwtUtil jwt = new JwtUtil(CLAVE_A, 60_000);
        String token = jwt.generarToken("user@universidad.edu", "ROLE_USER");

        assertThat(token).isNotBlank().contains(".");
        assertThat(jwt.esValido(token)).isTrue();
        assertThat(jwt.extraerEmail(token)).isEqualTo("user@universidad.edu");
        assertThat(jwt.extraerRol(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        String token = new JwtUtil(CLAVE_B, 60_000).generarToken("x@universidad.edu", "ROLE_ADMIN");
        assertThat(new JwtUtil(CLAVE_A, 60_000).esValido(token)).isFalse();
    }

    @Test
    void rechazaTokenConFirmaAlterada() {
        JwtUtil jwt = new JwtUtil(CLAVE_A, 60_000);
        String token = jwt.generarToken("user@universidad.edu", "ROLE_USER");
        assertThat(jwt.esValido(alterarFirma(token))).isFalse();
    }

    @Test
    void rechazaTokenConPayloadAlterado() {
        JwtUtil jwt = new JwtUtil(CLAVE_A, 60_000);
        String[] partes = jwt.generarToken("user@universidad.edu", "ROLE_USER").split("\\.");
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"user@universidad.edu\",\"rol\":\"ROLE_ADMIN\"}".getBytes());
        assertThat(jwt.esValido(partes[0] + "." + payloadFalso + "." + partes[2])).isFalse();
    }

    @Test
    void rechazaTokenExpirado() {
        JwtUtil jwt = new JwtUtil(CLAVE_A, -1_000); 
        String token = jwt.generarToken("user@universidad.edu", "ROLE_USER");
        assertThat(jwt.esValido(token)).isFalse();
    }

    @Test
    void rechazaTokenMalformadoOVacio() {
        JwtUtil jwt = new JwtUtil(CLAVE_A, 60_000);
        assertThat(jwt.esValido("eyJhbGciOiJIUzI1NiJ9.token.modificado")).isFalse();
        assertThat(jwt.esValido("no-es-un-jwt")).isFalse();
        assertThat(jwt.esValido("")).isFalse();
    }

    @Test
    void exigeClaveDeAlMenos32Bytes() {
        assertThatThrownBy(() -> new JwtUtil("corta", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    static String alterarFirma(String token) {
        int i = token.lastIndexOf('.') + 10;
        char c = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + c + token.substring(i + 1);
    }
}
