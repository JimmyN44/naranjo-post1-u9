package com.universidad.seguridad.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String ROL_ADMIN = "ROLE_ADMIN";
    public static final String ROL_USER = "ROLE_USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Email(message = "Debe ser un correo válido")
    @NotBlank(message = "El correo es obligatorio")
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String contrasenia; // BCrypt hash - nunca texto claro

    // "ROLE_ADMIN" o "ROLE_USER"
    @NotBlank
    @Column(nullable = false, length = 20)
    private String rol;

    private boolean activo = true;

    public Usuario() {}

    public Usuario(String nombre, String email, String contrasenia, String rol) {
        this.nombre = nombre;
        this.email = email;
        this.contrasenia = contrasenia;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean esAdmin() { return ROL_ADMIN.equals(rol); }
}
