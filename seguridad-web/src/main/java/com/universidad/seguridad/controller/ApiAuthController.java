package com.universidad.seguridad.controller;

import com.universidad.seguridad.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public ApiAuthController(AuthenticationManager authManager, JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String contrasenia) {}

    public record LoginResponse(String token, String email, String rol) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // Reutiliza el MISMO AuthenticationManager/UserDetailsService/
        // BCryptPasswordEncoder de la Parte 1: las credenciales se
        // validan exactamente igual, solo cambia lo que se devuelve
        // al final (un token en vez de una cookie de sesion).
        // Si las credenciales no son validas se lanza AuthenticationException,
        // que ApiExceptionHandler traduce a 401 sin emitir token.
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.contrasenia())
        );
        String email = auth.getName();
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        String token = jwtUtil.generarToken(email, rol);
        return new LoginResponse(token, email, rol);
    }
}
