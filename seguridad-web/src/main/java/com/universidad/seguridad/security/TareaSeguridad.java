package com.universidad.seguridad.security;

import com.universidad.seguridad.repository.TareaRepository;
import com.universidad.seguridad.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("tareaSeguridad")
public class TareaSeguridad {

    private final UsuarioRepository usuarioRepo;
    private final TareaRepository tareaRepo;

    public TareaSeguridad(UsuarioRepository usuarioRepo, TareaRepository tareaRepo) {
        this.usuarioRepo = usuarioRepo;
        this.tareaRepo = tareaRepo;
    }

    @Transactional(readOnly = true)
    public boolean esMismoUsuario(Long usuarioId, Authentication auth) {
        if (usuarioId == null || auth == null) {
            return false;
        }
        return usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getId().equals(usuarioId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean esPropietarioDeTarea(Long tareaId, Authentication auth) {
        if (tareaId == null || auth == null) {
            return false;
        }
        return tareaRepo.findById(tareaId)
                .map(t -> t.getPropietario().getEmail().equals(auth.getName()))
                .orElse(false);
    }
}
