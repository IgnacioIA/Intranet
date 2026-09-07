package com.IntraNet.Laucom.security.infrastructure.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Carga el par de claves RSA (asimétrico, ADR-009) usado para firmar/verificar el Access Token.
 * Formato de configuración: DER codificado en Base64 (PKCS8 para la privada, X.509 para la
 * pública) — se evita el formato PEM para no lidiar con saltos de línea/encabezados dentro de
 * variables de entorno. Sin valores por defecto: faltar cualquiera de las dos claves es un error
 * de configuración que debe fallar rápido al arrancar (REQ-AUTH-026: sin secretos en el código,
 * y sin un valor de repuesto inseguro).
 */
@Component
class JwtSigningKeys {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;

    JwtSigningKeys(@Value("${jwt.private-key-base64}") String privateKeyBase64,
                   @Value("${jwt.public-key-base64}") String publicKeyBase64,
                   @Value("${jwt.kid}") String keyId) {
        this.privateKey = decodePrivateKey(privateKeyBase64);
        this.publicKey = decodePublicKey(publicKeyBase64);
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("jwt.kid no puede estar vacío (ADR-009: kid obligatorio desde V1)");
        }
        this.keyId = keyId;
    }

    PrivateKey privateKey() {
        return privateKey;
    }

    PublicKey publicKey() {
        return publicKey;
    }

    String keyId() {
        return keyId;
    }

    private static PrivateKey decodePrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(requireConfigured(base64, "jwt.private-key-base64"));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "jwt.private-key-base64 inválida: se espera una clave privada RSA PKCS8 en Base64", e);
        }
    }

    private static PublicKey decodePublicKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(requireConfigured(base64, "jwt.public-key-base64"));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "jwt.public-key-base64 inválida: se espera una clave pública RSA X.509 en Base64", e);
        }
    }

    private static String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " no está configurada (obligatoria, sin valor por defecto)");
        }
        return value;
    }
}
