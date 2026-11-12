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
       
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return service.listarDe(usuario.getId()).stream().map(TareaDTO::desde).toList();
    }

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
