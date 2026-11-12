package com.universidad.seguridad.exception;

/** Se lanza al intentar registrar un correo que ya existe. */
public class EmailDuplicadoException extends RuntimeException {

    public EmailDuplicadoException(String email) {
        super("El correo ya está registrado: " + email);
    }
}
