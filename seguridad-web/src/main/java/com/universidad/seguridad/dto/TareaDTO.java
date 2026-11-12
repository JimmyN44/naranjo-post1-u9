package com.universidad.seguridad.dto;

import com.universidad.seguridad.model.Tarea;

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
