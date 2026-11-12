package com.universidad.seguridad.controller;

import com.universidad.seguridad.dto.TareaForm;
import com.universidad.seguridad.model.Tarea;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.service.TareaService;
import com.universidad.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/tareas")
public class TareaController {

    private final TareaService tareaService;
    private final UsuarioService usuarioService;

    public TareaController(TareaService tareaService, UsuarioService usuarioService) {
        this.tareaService = tareaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "q", required = false) String q,
                         Model model, Authentication auth) {
        Usuario actual = usuarioService.buscarPorEmail(auth.getName());
        boolean admin = actual.esAdmin();
        boolean hayBusqueda = q != null && !q.isBlank();

        List<Tarea> tareas;
        if (hayBusqueda) {
            tareas = admin ? tareaService.buscar(q) : tareaService.buscarDe(q, actual.getId());
        } else {
            tareas = admin ? tareaService.listarTodas() : tareaService.listarDe(actual.getId());
        }

        model.addAttribute("tareas", tareas);
        model.addAttribute("q", q);
        model.addAttribute("esAdmin", admin);
        if (!model.containsAttribute("tareaForm")) {
            model.addAttribute("tareaForm", new TareaForm());
        }
        return "tareas/lista";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("tareaForm") TareaForm tareaForm,
                        BindingResult result, Authentication auth,
                        Model model, RedirectAttributes redirect) {
        Usuario actual = usuarioService.buscarPorEmail(auth.getName());
        if (result.hasErrors()) {
            boolean admin = actual.esAdmin();
            model.addAttribute("tareas",
                    admin ? tareaService.listarTodas() : tareaService.listarDe(actual.getId()));
            model.addAttribute("esAdmin", admin);
            return "tareas/lista";
        }
        tareaService.crear(tareaForm, actual);
        redirect.addFlashAttribute("mensaje", "Tarea creada");
        return "redirect:/tareas";
    }

    @PostMapping("/{id}/completar")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirect) {
        tareaService.cambiarEstado(id);
        redirect.addFlashAttribute("mensaje", "Estado de la tarea actualizado");
        return "redirect:/tareas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @RequestParam(name = "origen", required = false) String origen,
                           RedirectAttributes redirect) {
        tareaService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Tarea eliminada");
        return "admin".equals(origen) ? "redirect:/admin" : "redirect:/tareas";
    }
}
