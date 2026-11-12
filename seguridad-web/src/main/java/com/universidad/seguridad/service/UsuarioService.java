package com.universidad.seguridad.service;

import com.universidad.seguridad.dto.RegistroForm;
import com.universidad.seguridad.exception.EmailDuplicadoException;
import com.universidad.seguridad.exception.RecursoNoEncontradoException;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /**
     * Registra un usuario nuevo: la contrasena se guarda SIEMPRE como hash
     * BCrypt y el rol se fija en el servidor (ROLE_USER), nunca desde el formulario.
     */
    @Transactional
    public Usuario registrar(RegistroForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (repo.existsByEmail(email)) {
            throw new EmailDuplicadoException(email);
        }
        Usuario usuario = new Usuario(
                form.getNombre(),
                email,
                encoder.encode(form.getContrasenia()),
                Usuario.ROL_USER); // rol por defecto
        return repo.save(usuario);
    }

    /** Crea un ADMIN (usado solo por el inicializador opcional). */
    @Transactional
    public Usuario crearAdmin(String nombre, String email, String contraseniaPlana) {
        String normalizado = email.trim().toLowerCase();
        if (repo.existsByEmail(normalizado)) {
            throw new EmailDuplicadoException(normalizado);
        }
        return repo.save(new Usuario(nombre, normalizado,
                encoder.encode(contraseniaPlana), Usuario.ROL_ADMIN));
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return repo.existsByEmail(email.trim().toLowerCase());
    }

    // Defensa en profundidad: ademas de la regla /admin/** de HttpSecurity,
    // el metodo de servicio exige el rol ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return repo.findAll();
    }
}
