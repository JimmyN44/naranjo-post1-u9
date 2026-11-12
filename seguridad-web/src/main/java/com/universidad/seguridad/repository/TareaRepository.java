package com.universidad.seguridad.repository;

import com.universidad.seguridad.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByPropietarioId(Long usuarioId);

  
    @Query("SELECT t FROM Tarea t WHERE t.titulo LIKE %:texto%")
    List<Tarea> buscarPorTitulo(@Param("texto") String texto);

    
    @Query("SELECT t FROM Tarea t WHERE t.propietario.id = :usuarioId AND t.titulo LIKE %:texto%")
    List<Tarea> buscarPorTituloYPropietario(@Param("texto") String texto,
                                            @Param("usuarioId") Long usuarioId);

}
