package com.universidad.seguridad.service;

import com.universidad.seguridad.dto.TareaForm;
import com.universidad.seguridad.exception.RecursoNoEncontradoException;
import com.universidad.seguridad.model.Tarea;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.repository.TareaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de acceso a tareas (se aplican igual con sesion o con JWT):
 *  - ADMIN: ve, busca, completa y elimina TODAS las tareas.
 *  - USER : solo ve, busca, completa y elimina SUS propias tareas.
 *
 * Nota sobre la guia: la expresion original
 *   "hasRole('ADMIN') or #usuarioId == authentication.principal.id"
 * falla en tiempo de ejecucion, porque el principal es un
 * org.springframework.security.core.userdetails.User (sesion) o un String
 * (JWT) y ninguno tiene la propiedad "id". Se delega la comprobacion en
 * el bean "tareaSeguridad", que compara contra authentication.name (email),
 * valido para ambos mecanismos de autenticacion.
 */
@Service
public class TareaService {

    private static final Sort POR_ID_DESC = Sort.by(Sort.Direction.DESC, "id");

    private final TareaRepository repo;

    public TareaService(TareaRepository repo) {
        this.repo = repo;
    }

    // ADMIN ve las de cualquier usuario; USER solo las suyas
    @PreAuthorize("hasRole('ADMIN') or @tareaSeguridad.esMismoUsuario(#usuarioId, authentication)")
    @Transactional(readOnly = true)
    public List<Tarea> listarDe(Long usuarioId) {
        return repo.findByPropietarioId(usuarioId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Tarea> listarTodas() {
        return repo.findAll(POR_ID_DESC);
    }

    // Busqueda global (ADMIN) - @Query parametrizado, seguro
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Tarea> buscar(String texto) {
        return repo.buscarPorTitulo(texto);
    }

    // Busqueda restringida al propietario - @Query parametrizado, seguro
    @PreAuthorize("hasRole('ADMIN') or @tareaSeguridad.esMismoUsuario(#usuarioId, authentication)")
    @Transactional(readOnly = true)
    public List<Tarea> buscarDe(String texto, Long usuarioId) {
        return repo.buscarPorTituloYPropietario(texto, usuarioId);
    }

    // El propietario lo asigna el controlador a partir del usuario autenticado,
    // nunca a partir de un dato enviado por el cliente.
    @Transactional
    public Tarea crear(TareaForm form, Usuario propietario) {
        Tarea tarea = new Tarea(form.getTitulo().trim(), form.getDescripcion(), propietario);
        return repo.save(tarea);
    }

    @PreAuthorize("hasRole('ADMIN') or @tareaSeguridad.esPropietarioDeTarea(#id, authentication)")
    @Transactional
    public Tarea cambiarEstado(Long id) {
        Tarea tarea = obtener(id);
        tarea.setCompletada(!tarea.isCompletada());
        return tarea;
    }

    @PreAuthorize("hasRole('ADMIN') or @tareaSeguridad.esPropietarioDeTarea(#id, authentication)")
    @Transactional
    public void eliminar(Long id) {
        repo.delete(obtener(id));
    }

    private Tarea obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada: " + id));
    }
}
