package com.universidad.seguridad.config;

import com.universidad.seguridad.security.ApiErrorWriter;
import com.universidad.seguridad.security.JwtAuthFilter;
import com.universidad.seguridad.security.UsuarioDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // factor de trabajo 12 -> hashes $2a$12$
    }

    @Bean
    public DaoAuthenticationProvider authProvider(UsuarioDetailsService uds, PasswordEncoder pe) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(pe);
        return p;
    }

    // La guia inyecta AuthenticationManager en ApiAuthController (Parte 2)
    // pero no declara el bean, por lo que la aplicacion no arrancaria.
    // Se expone aqui, construido sobre el MISMO DaoAuthenticationProvider
    // (BD + BCrypt) que usa el form-login.
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authProvider) {
        return new ProviderManager(authProvider);
    }

    // JwtAuthFilter es @Component: Spring Boot lo registraria ademas como
    // filtro global de Servlets (todas las rutas). Se desactiva ese registro
    // para que SOLO se ejecute dentro de apiFilterChain.
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    // Cadena de filtros para la API REST - stateless con JWT.
    // @Order(1): se evalua ANTES que webFilterChain (@Order(2)): mas
    // especifico primero, siguiendo la practica recomendada de Spring
    // Security para multiples SecurityFilterChain.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthFilter jwtAuthFilter,
                                              ApiErrorWriter apiErrorWriter) throws Exception {
        http
            .securityMatcher("/api/**") // esta cadena SOLO aplica a /api/**
            .csrf(csrf -> csrf.disable()) // correcto aqui: no hay cookie de
                                          // sesion que un atacante pueda
                                          // aprovechar; el token va en un
                                          // header que un formulario cross-
                                          // site no puede fijar por si solo
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // 401 (no 403) cuando falta el token o es invalido/expirado;
            // 403 cuando el token es valido pero el rol no alcanza.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiErrorWriter.entryPoint())
                .accessDeniedHandler(apiErrorWriter.accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Cadena de filtros para las vistas web con Thymeleaf - basada en
    // sesion. @Order(2): se evalua despues de la cadena de /api/** de la
    // Parte 2, que tiene @Order(1) y mayor prioridad. Sin @Order explicito,
    // Spring Security no garantiza cual cadena evalua primero cuando hay
    // mas de un SecurityFilterChain.
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**") // todo lo que no sea /api/**
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/registro",
                                 "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
            // CSRF queda ACTIVO (comportamiento por defecto de Spring
            // Security): esta cadena sirve formularios Thymeleaf con
            // cookie de sesion, el escenario exacto que CSRF protege.
        return http.build();
    }
}
