package com.universidad.seguridad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA (jjwt 0.12.5).
 *
 * Diferencia deliberada con la guia: la clave NO esta hardcodeada en el
 * codigo. Se inyecta desde la propiedad jwt.secret, que a su vez lee la
 * variable de entorno JWT_SECRET (ver application.properties). Asi se cumple
 * la rubrica ("sin claves secretas hardcodeadas"). La expiracion tambien es
 * configurable (jwt.expiracion-ms, 1 hora por defecto) para poder probar
 * el rechazo de tokens expirados.
 */
@Component
public class JwtUtil {

    private static final int LONGITUD_MINIMA_CLAVE = 32; // 256 bits para HS256

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtUtil(@Value("${jwt.secret}") String secreto,
                   @Value("${jwt.expiracion-ms:3600000}") long expiracionMs) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                    "JWT_SECRET debe definirse con al menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String email, String rol) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expiracionMs))
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return parsearClaims(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            parsearClaims(token); // lanza excepcion si la firma o la
                                  // expiracion no son validas
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // firma invalida, token expirado o malformado
        }
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
