package com.IntraNet.Laucom.security.domain.port;

/**
 * Hashing/verificación de contraseñas {@code LOCAL}. Ver ADR-011 (Argon2id).
 */
public interface PasswordHasherPort {

    String hash(char[] plainPassword);

    boolean matches(char[] plainPassword, String hash);
}
