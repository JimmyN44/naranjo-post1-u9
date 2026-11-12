package com.universidad.seguridad.controller;

import com.universidad.seguridad.dto.TareaDTO;
import com.universidad.seguridad.dto.UsuarioDTO;
import com.universidad.seguridad.service.TareaService;
import com.universidad.seguridad.service.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private final UsuarioService usuarioService;
    private final TareaService tareaService;

    public ApiAdminController(UsuarioService usuarioService, TareaService tareaService) {
        this.usuarioService = usuarioService;
        this.tareaService = tareaService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioDTO> usuarios() {
        return usuarioService.listarTodos().stream().map(UsuarioDTO::desde).toList();
    }

    @GetMapping("/tareas")
    public List<TareaDTO> tareas(@RequestParam(name = "texto", required = false) String texto) {
        var tareas = (texto == null || texto.isBlank())
                ? tareaService.listarTodas()
                : tareaService.buscar(texto);
        return tareas.stream().map(TareaDTO::desde).toList();
    }
}
