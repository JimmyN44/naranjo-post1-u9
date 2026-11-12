package com.universidad.seguridad.security;

import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario desde MySQL para Spring Security.
 * Lo usan tanto el form-login (Parte 1) como POST /api/auth/login (Parte 2).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public UsuarioDetailsService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizado = email == null ? "" : email.trim().toLowerCase();
        Usuario u = repo.findByEmail(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + normalizado));

        return User.builder()
                .username(u.getEmail())
                .password(u.getContrasenia())               // BCrypt hash
                .roles(u.getRol().replace("ROLE_", ""))     // "ADMIN" o "USER"
                .disabled(!u.isActivo())
                .build();
    }
}
