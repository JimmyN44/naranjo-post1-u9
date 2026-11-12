package com.universidad.seguridad.controller;

import com.universidad.seguridad.dto.RegistroForm;
import com.universidad.seguridad.exception.EmailDuplicadoException;
import com.universidad.seguridad.model.Usuario;
import com.universidad.seguridad.service.TareaService;
import com.universidad.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UsuarioService service;
    private final TareaService tareaService;

    public AuthController(UsuarioService service, TareaService tareaService) {
        this.service = service;
        this.tareaService = tareaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registro", new RegistroForm());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroForm registro,
                            BindingResult result) {
        if (result.hasErrors()) {
            return "auth/registro";
        }
        try {
            service.registrar(registro);
            return "redirect:/login?registrado";
        } catch (EmailDuplicadoException e) {
            result.rejectValue("email", "error.email", e.getMessage());
            return "auth/registro";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        // La guia pasaba auth.getName() (el email), que nunca puede contener
        // un payload XSS porque @Email lo rechaza. Se pasa el NOMBRE del
        // usuario, campo de texto libre, para que la prueba XSS sea real.
        Usuario usuario = service.buscarPorEmail(auth.getName());
        model.addAttribute("usuario", usuario.getNombre());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("roles", auth.getAuthorities());
        return "dashboard";
    }

    @GetMapping("/admin")
    public String adminPanel(Model model) {
        model.addAttribute("usuarios", service.listarTodos());
        model.addAttribute("tareas", tareaService.listarTodas());
        return "admin/panel";
    }
}
