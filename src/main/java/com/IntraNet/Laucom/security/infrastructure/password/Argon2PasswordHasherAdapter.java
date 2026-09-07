package com.IntraNet.Laucom.security.infrastructure.password;

import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ADR-011: Argon2id. Parámetros externalizados (REQ-AUTH-019) con los valores por defecto
 * recomendados por Spring Security 5.8+ (`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8`)
 * como configuración de fábrica.
 *
 * <p>Requiere BouncyCastle en el classpath en runtime (lo agrega Spring Security internamente
 * al delegar el algoritmo Argon2 concreto).</p>
 */
@Component
public class Argon2PasswordHasherAdapter implements PasswordHasherPort {

    private final Argon2PasswordEncoder encoder;

    public Argon2PasswordHasherAdapter(
            @Value("${security.password.argon2.salt-length:16}") int saltLength,
            @Value("${security.password.argon2.hash-length:32}") int hashLength,
            @Value("${security.password.argon2.parallelism:1}") int parallelism,
            @Value("${security.password.argon2.memory-kb:16384}") int memoryKb,
            @Value("${security.password.argon2.iterations:2}") int iterations) {
        this.encoder = new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKb, iterations);
    }

    @Override
    public String hash(char[] plainPassword) {
        // Argon2PasswordEncoder opera sobre CharSequence/String; no existe una API de Spring
        // Security que evite este String intermedio (limitación aceptada, ver ADR-011).
        return encoder.encode(new String(plainPassword));
    }

    @Override
    public boolean matches(char[] plainPassword, String hash) {
        return encoder.matches(new String(plainPassword), hash);
    }
}
