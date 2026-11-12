package com.universidad.seguridad.controller;

import com.universidad.seguridad.dto.TareaDTO;
import com.universidad.seguridad.dto.TareaForm;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.service.TareaService;
import com.universidad.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tareas del usuario autenticado por JWT. Las reglas de propiedad
 * (@PreAuthorize en TareaService) son las mismas que en las vistas web.
 */
@RestController
@RequestMapping("/api/tareas")
public class ApiTareaController {

    private final TareaService service;
    private final UsuarioService usuarioService;

    public ApiTareaController(TareaService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<TareaDTO> misTareas(Authentication auth) {
        // Authentication llega poblado por JwtAuthFilter, exactamente
        // como llegaria si la peticion viniera autenticada por sesion:
        // el resto de la aplicacion no necesita saber cual mecanismo
        // se uso para autenticar a este usuario.
        // (La guia dejaba "Usuario usuario = null"; aqui se busca por email.)
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return service.listarDe(usuario.getId()).stream().map(TareaDTO::desde).toList();
    }

    // Busqueda con @Query parametrizado, restringida a las tareas propias
    @GetMapping("/buscar")
    public List<TareaDTO> buscar(@RequestParam("texto") String texto, Authentication auth) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return service.buscarDe(texto, usuario.getId()).stream().map(TareaDTO::desde).toList();
    }

    @PostMapping
    public ResponseEntity<TareaDTO> crear(@Valid @RequestBody TareaForm form, Authentication auth) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        TareaDTO creada = TareaDTO.desde(service.crear(form, usuario));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Propietario o ADMIN (verificado por @PreAuthorize en el servicio)
    @PatchMapping("/{id}/completar")
    public TareaDTO cambiarEstado(@PathVariable Long id) {
        return TareaDTO.desde(service.cambiarEstado(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
