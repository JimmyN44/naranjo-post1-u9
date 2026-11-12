package com.universidad.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del formulario para crear una tarea (el propietario lo fija el servidor). */
public class TareaForm {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título admite como máximo 150 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción admite como máximo 500 caracteres")
    private String descripcion;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
