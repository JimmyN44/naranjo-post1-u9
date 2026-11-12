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

    // SEGURO: @Query con parametro nombrado - JPA lo envia como
    // PreparedStatement, el valor nunca se interpreta como JPQL/SQL.
    // Uso: ADMIN busca entre TODAS las tareas.
    @Query("SELECT t FROM Tarea t WHERE t.titulo LIKE %:texto%")
    List<Tarea> buscarPorTitulo(@Param("texto") String texto);

    // SEGURO: misma busqueda parametrizada, restringida al propietario.
    // Uso: USER solo busca entre SUS tareas (evita filtrar tareas ajenas).
    @Query("SELECT t FROM Tarea t WHERE t.propietario.id = :usuarioId AND t.titulo LIKE %:texto%")
    List<Tarea> buscarPorTituloYPropietario(@Param("texto") String texto,
                                            @Param("usuarioId") Long usuarioId);

    // INSEGURO - NUNCA HACER ESTO (solo referencia en comentario):
    // @Query(value = "SELECT * FROM tareas WHERE titulo LIKE '%" +
    //        texto + "%'", nativeQuery = true)
    // Si texto = "' OR '1'='1", la consulta devuelve TODAS las tareas
    // sin filtrar, ignorando por completo el criterio de busqueda.
}
