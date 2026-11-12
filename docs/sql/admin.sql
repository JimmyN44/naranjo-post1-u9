INSERT INTO usuarios (nombre, email, contrasenia, rol, activo)
VALUES ('Administrador', 'admin@universidad.edu', '<HASH_GENERADO>', 'ROLE_ADMIN', 1);

SELECT id, nombre, email, LEFT(contrasenia, 7) AS prefijo_hash, rol, activo FROM usuarios;
