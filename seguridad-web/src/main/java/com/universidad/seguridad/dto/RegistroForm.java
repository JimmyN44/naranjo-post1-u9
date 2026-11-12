package com.universidad.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de registro.
 *
 * Se usa un DTO en lugar de enlazar directamente la entidad Usuario
 * (como hace la guia) para evitar "mass assignment": con la entidad,
 * un atacante podria enviar campos ocultos como id, rol o activo
 * (p. ej. id=1 haria que save() sobrescribiera al ADMIN). Aqui solo
 * existen los tres campos que el usuario debe poder escribir.
 */
public class RegistroForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite como máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ser un correo válido")
    @Size(max = 150, message = "El correo admite como máximo 150 caracteres")
    private String email;

    // BCrypt solo procesa los primeros 72 bytes de la contrasena
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String contrasenia;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }
}
