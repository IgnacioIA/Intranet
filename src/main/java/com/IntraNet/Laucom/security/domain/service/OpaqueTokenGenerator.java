package com.IntraNet.Laucom.security.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generación y hashing de tokens opacos de alta entropía (Refresh Token, Password Recovery
 * Token). Distinto de {@link com.IntraNet.Laucom.security.domain.port.PasswordHasherPort}
 * (Argon2id): estos valores ya tienen alta entropía por construcción, por lo que un hash
 * rápido (SHA-256) es suficiente y apropiado — ADR-011 solo exige un KDF lento para
 * contraseñas de bajo entropía elegidas por humanos.
 *
 * <p>Utilidad de dominio pura (solo JDK estándar): no requiere un Port propio porque no existe
 * una implementación alternativa plausible que deba poder sustituirse (evita sobreingeniería).</p>
 */
public final class OpaqueTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OpaqueTokenGenerator() {
    }

    /** Valor en claro, entregado al usuario (por cookie, email, etc.) — nunca se persiste tal cual. */
    public static String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Valor persistido en base de datos — nunca el secreto en claro (REQ-AUTH-020). */
    public static String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
