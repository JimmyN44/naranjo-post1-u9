package com.universidad.seguridad.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el token JWT en CADA peticion a /api/** (cadena apiFilterChain).
 *
 * Nota: al ser @Component, Spring Boot lo registraria tambien como filtro
 * global del contenedor de Servlets (se ejecutaria en todas las rutas,
 * incluidas las vistas web). SecurityConfig desactiva ese registro con un
 * FilterRegistrationBean, de modo que solo actua dentro de la cadena /api/**.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIJO)) {
            String token = header.substring(PREFIJO.length());

            if (jwtUtil.esValido(token)) {
                String email = jwtUtil.extraerEmail(token);
                String rol = jwtUtil.extraerRol(token);

                var authToken = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority(rol)));
                // Puebla el SecurityContext manualmente: no hay sesion
                // de la que Spring Security pueda recuperar el usuario
                // autenticado en la siguiente peticion, asi que este
                // filtro se ejecuta en CADA peticion a /api/**.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            // Si el token no es valido, simplemente no se autentica: la
            // SecurityFilterChain decide si la ruta requeria autenticacion
            // y responde 401 en ese caso.
        }

        chain.doFilter(request, response);
    }
}
