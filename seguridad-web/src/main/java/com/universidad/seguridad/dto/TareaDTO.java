package com.universidad.seguridad.dto;

import com.universidad.seguridad.model.Tarea;

/**
 * Representacion JSON de una tarea para la API REST.
 *
 * La guia devuelve la entidad Tarea directamente; eso serializaria el
 * Usuario propietario completo, incluido el hash BCrypt de su contrasena.
 * Este DTO expone solo los datos necesarios del propietario.
 */
public record TareaDTO(Long id,
                       String titulo,
                       String descripcion,
                       boolean completada,
                       Long propietarioId,
                       String propietarioEmail) {

    public static TareaDTO desde(Tarea t) {
        return new TareaDTO(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.isCompletada(),
                t.getPropietario().getId(),
                t.getPropietario().getEmail());
    }
}
