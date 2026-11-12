-- ------------------------------------------------------------------
-- Creacion de la base de datos y del usuario ADMIN (Parte 1, Paso 8)
-- ------------------------------------------------------------------
-- 1) (como root) Crear la base de datos y el usuario de la aplicacion.
--    Reemplace <CONTRASENA_MYSQL> por una contrasena propia; NO la suba
--    al repositorio: se entrega a la aplicacion con la variable DB_PASSWORD.
--
-- CREATE DATABASE IF NOT EXISTS seguridad_db
--     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY '<CONTRASENA_MYSQL>';
-- GRANT ALL PRIVILEGES ON seguridad_db.* TO 'appuser'@'localhost';
-- FLUSH PRIVILEGES;
--
-- 2) Arrancar la aplicacion una vez para que Hibernate cree las tablas
--    (spring.jpa.hibernate.ddl-auto=update).
--
-- 3) Generar el hash BCrypt (factor 12) de la contrasena del ADMIN sin
--    escribirla en el codigo fuente (desde la carpeta seguridad-web/):
--
--    ./mvnw test -Dtest=GenerarHashTest -Dpassword='<CONTRASENA_ADMIN>'
--
--    La consola muestra una linea "HASH BCRYPT: $2a$12$...".
--
-- 4) Insertar el ADMIN (mysql -u appuser -p seguridad_db) reemplazando
--    <HASH_GENERADO> por el hash del paso anterior. Se usan comillas
--    simples (estandar SQL) en lugar de las dobles de la guia.
-- ------------------------------------------------------------------

INSERT INTO usuarios (nombre, email, contrasenia, rol, activo)
VALUES ('Administrador', 'admin@universidad.edu', '<HASH_GENERADO>', 'ROLE_ADMIN', 1);

-- Verificacion: la contrasena guardada debe empezar por $2a$12$
SELECT id, nombre, email, LEFT(contrasenia, 7) AS prefijo_hash, rol, activo FROM usuarios;
